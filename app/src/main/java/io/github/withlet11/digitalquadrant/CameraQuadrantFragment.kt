/**
 * CameraQuadrantFragment.kt
 *
 * Copyright 2020 Yasuhiro Yamakawa <withlet11@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.withlet11.digitalquadrant


import android.Manifest
// import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.util.Size
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max


class CameraQuadrantFragment : QuadrantFragment(), OnRequestPermissionsResultCallback {
    companion object {
        const val REQUEST_CAMERA_PERMISSION = 1
        const val FRAGMENT_DIALOG = "dialog"
        const val TAG = "CameraQuadrantFragment"

        const val MAX_PREVIEW_WIDTH =
            1920 // Max preview width that is guaranteed by Camera2 API
        const val MAX_PREVIEW_HEIGHT =
            1080 // Max preview height that is guaranteed by Camera2 API

        fun chooseOptimalSize(
            choices: Array<Size>, textureViewWidth: Int,
            textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspect: Size
        ): Size {
            val larger: MutableList<Size> = ArrayList()
            val smaller: MutableList<Size> = ArrayList()

            choices.forEach {
                if (it.width <= maxWidth &&
                    it.height <= maxHeight &&
                    it.height == it.width * aspect.height / aspect.width
                ) {
                    (if (it.width >= textureViewWidth && it.height >= textureViewHeight) larger else smaller).add(
                        it
                    )
                }
            }

            return when {
                larger.size > 0 -> Collections.min(larger, CompareSizesByArea())
                smaller.size > 0 -> Collections.max(
                    smaller,
                    CompareSizesByArea()
                )
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }
    }

    private val surfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    private var cameraId = ""
    private var cameraView: CameraView? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var previewSize: Size? = null
    private var isManualFocusSupported = false
    private var maxISO = 100

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            cameraOpenCloseLock.release()
            createCameraPreviewSession(device)
        }

        override fun onDisconnected(device: CameraDevice) {
            cameraOpenCloseLock.release()
            device.close()
            cameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            device.close()
            cameraDevice = null
            activity?.finish()
        }
    }

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null

    private val cameraOpenCloseLock = Semaphore(1)
    private var isFlashSupported = false
    private var sensorOrientation = 0


    private val captureCallback: CaptureCallback = object : CaptureCallback() {
        private fun process(result: CaptureResult) {
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }
    }

    private lateinit var reticleView: ReticleView
    // private val handler = Handler()
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private lateinit var runnable: Runnable

    private fun showToast(text: String) {
        activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_camera_quadrant, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraView = view.findViewById<View>(R.id.cameraView) as CameraView

        reticleView = view.findViewById(R.id.reticleView) as ReticleView
        reticleView.setOnClickListener { reticleView.togglePause() }
        reticleView.setZOrderOnTop(true)
    }

    override fun onResume() {
        super.onResume()
        timerSet()

        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        cameraView!!.let { view ->
            if (view.isAvailable) {
                val viewSize = view.aspect
                val layoutParams = reticleView.layoutParams
                layoutParams.width = viewSize.width
                layoutParams.height = viewSize.height
                reticleView.layoutParams = layoutParams
                openCamera(view.width, view.height)
            } else {
                view.surfaceTextureListener = surfaceTextureListener
            }
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        stopTimerTask()
        super.onPause()
    }

    private fun timerSet() {
        runnable = object : Runnable {
            override fun run() {
                if (isStable) {
                    if (!reticleView.isPaused) reticleView.togglePause()
                } else {
                    reticleView.setPosition(pitchZ, rollZ)
                }
                handler.postDelayed(this, PERIOD)
            }
        }
        // handler.post(runnable)
        handler.postDelayed(runnable, PERIOD)
    }

    private fun stopTimerTask() {
        handler.removeCallbacks(runnable)
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.requestPermission))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager =
            activity?.applicationContext?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context?.display
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay
        }

        try {
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)

                // Don't use a front facing camera
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )
                    ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    listOf(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )


                imageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG,  /*maxImages*/2
                )

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = display?.rotation ?: 0
                sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                var swappedDimensions = false
                when (displayRotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }
                    else -> Log.e(
                        TAG,
                        "Display rotation is invalid: $displayRotation"
                    )
                }
                val displaySize = Point()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context?.getSystemService(WindowManager::class.java)?.let { windowManager ->
                        val metrics: WindowMetrics = windowManager.currentWindowMetrics
                        val windowInsets: WindowInsets = metrics.windowInsets
                        val insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
                        val insetsWidth = insets.right + insets.left
                        val insetsHeight = insets.top + insets.bottom
                        val bounds = metrics.bounds
                        displaySize.set(bounds.width() - insetsWidth, bounds.height() - insetsHeight)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    display?.getSize(displaySize)
                }

                var rotatedPreviewWidth = width
                var rotatedPreviewHeight = height
                var maxPreviewWidth = displaySize.x
                var maxPreviewHeight = displaySize.y
                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize.y
                    maxPreviewHeight = displaySize.x
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }

                previewSize = chooseOptimalSize(
                    map.getOutputSizes(
                        SurfaceTexture::class.java
                    ),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest
                ).also { size ->
                    // We fit the aspect ratio of TextureView to the size of preview we picked.
                    val orientation: Int = resources.configuration.orientation
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        cameraView!!.aspect = Size(size.width, size.height)
                        val layoutParams = reticleView.layoutParams
                        layoutParams.width = size.width
                        layoutParams.height = size.height
                        reticleView.layoutParams = layoutParams
                    } else {
                        cameraView!!.aspect = Size(size.height, size.width)
                        val layoutParams = reticleView.layoutParams
                        layoutParams.width = size.height
                        layoutParams.height = size.width
                        reticleView.layoutParams = layoutParams
                    }
                }

                // Check if the manual focus is supported.
                val capabilities =
                    characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
                isManualFocusSupported =
                    capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                        ?: false

                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_MODE,
                    CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                )

                val range2 =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                maxISO = range2!!.upper //10000
                // val min1: Int = range2!!.getLower() //100

                // Check if the flash is supported.
                val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                isFlashSupported = available ?: false
                cameraId = id

                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            ErrorDialog.newInstance(getString(R.string.cameraError))
                .show(childFragmentManager, FRAGMENT_DIALOG)
        }
    }

    private fun openCamera(width: Int, height: Int) {
        activity?.applicationContext?.let { _activity ->
            if (ContextCompat.checkSelfPermission(_activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermission()
                return
            }
            setUpCameraOutputs(width, height)
            configureTransform(width, height)
            val manager = _activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw RuntimeException("Time out waiting to lock camera opening.")
                }
                manager.openCamera(cameraId, stateCallback, backgroundHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                throw RuntimeException("Interrupted while trying to lock camera opening.", e)
            }
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread!!.let { thread ->
            thread.quitSafely()
            try {
                thread.join()
                backgroundThread = null
                backgroundHandler = null
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun createCameraPreviewSession(device: CameraDevice) {
        cameraDevice = device

        try {
            val texture = cameraView!!.surfaceTexture!!

            // We configure the size of default buffer to be the size of camera preview we want.
            previewSize!!.run { texture.setDefaultBufferSize(width, height) }

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder =
                device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                }.also { requestBuilder ->
                    // Here, we create a CameraCaptureSession for camera preview.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val sessionContext = android.hardware.camera2.params.SessionConfiguration(SESSION_REGULAR,
                            arrayListOf(OutputConfiguration(surface), OutputConfiguration(imageReader!!.surface)),
                            Executors.newSingleThreadExecutor(),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                    // The camera is already closed
                                    cameraDevice?.let {
                                        // When the session is ready, we start displaying the preview.
                                        captureSession = cameraCaptureSession.also { session ->
                                            try {
                                                if (isManualFocusSupported) {
                                                    requestBuilder.set(
                                                        CaptureRequest.CONTROL_AF_MODE,
                                                        CaptureRequest.CONTROL_AF_MODE_OFF
                                                    )
                                                    requestBuilder.set(
                                                        CaptureRequest.LENS_FOCUS_DISTANCE,
                                                        0f
                                                    )
                                                } else {
                                                    requestBuilder.set(
                                                        CaptureRequest.CONTROL_AF_MODE,
                                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                                    )
                                                }
                                                // Flash is automatically enabled when necessary.
                                                // setAutoFlash(requestBuilder)

                                                // set max ISO
                                                requestBuilder.set(
                                                    CaptureRequest.SENSOR_SENSITIVITY,
                                                    maxISO
                                                )
                                                println("max ISO: $maxISO")

                                                // Finally, we start displaying the camera preview.
                                                previewRequest =
                                                    requestBuilder.build().also { request ->
                                                        session.setRepeatingRequest(
                                                            request,
                                                            captureCallback, backgroundHandler
                                                        )
                                                    }
                                            } catch (e: CameraAccessException) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }

                                override fun onConfigureFailed(
                                    cameraCaptureSession: CameraCaptureSession
                                ) {
                                    showToast("Failed")
                                }
                            }
                        )
                        device.createCaptureSession(sessionContext)
                    } else @Suppress("DEPRECATION") {
                        device.createCaptureSession(
                            listOf(surface, imageReader!!.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                    // The camera is already closed
                                    cameraDevice?.let {
                                        // When the session is ready, we start displaying the preview.
                                        captureSession = cameraCaptureSession.also { session ->
                                            try {
                                                if (isManualFocusSupported) {
                                                    requestBuilder.set(
                                                        CaptureRequest.CONTROL_AF_MODE,
                                                        CaptureRequest.CONTROL_AF_MODE_OFF
                                                    )
                                                    requestBuilder.set(
                                                        CaptureRequest.LENS_FOCUS_DISTANCE,
                                                        0f
                                                    )
                                                } else {
                                                    requestBuilder.set(
                                                        CaptureRequest.CONTROL_AF_MODE,
                                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                                    )
                                                }
                                                // Flash is automatically enabled when necessary.
                                                // setAutoFlash(requestBuilder)

                                                // set max ISO
                                                requestBuilder.set(
                                                    CaptureRequest.SENSOR_SENSITIVITY,
                                                    maxISO
                                                )
                                                println("max ISO: $maxISO")

                                                // Finally, we start displaying the camera preview.
                                                previewRequest =
                                                    requestBuilder.build().also { request ->
                                                        session.setRepeatingRequest(
                                                            request,
                                                            captureCallback, backgroundHandler
                                                        )
                                                    }
                                            } catch (e: CameraAccessException) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }

                                override fun onConfigureFailed(
                                    cameraCaptureSession: CameraCaptureSession
                                ) {
                                    showToast("Failed")
                                }
                            }, null
                        )
                    }
                }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        cameraView?.let { view ->
            previewSize?.let { size ->
                activity?.let { _activity ->
                    val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context?.display?.rotation
                    } else {
                        @Suppress("DEPRECATION")
                        _activity.windowManager.defaultDisplay.rotation
                    }
                    val matrix = Matrix()
                    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
                    val bufferRect = RectF(
                        0f, 0f, size.height.toFloat(),
                        size.width.toFloat()
                    )
                    val centerX = viewRect.centerX()
                    val centerY = viewRect.centerY()
                    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                        bufferRect.offset(
                            centerX - bufferRect.centerX(),
                            centerY - bufferRect.centerY()
                        )
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                        val scale = max(
                            viewHeight.toFloat() / size.height,
                            viewWidth.toFloat() / size.width
                        )
                        matrix.postScale(scale, scale, centerX, centerY)
                        matrix.postRotate(90 * (rotation - 2).toFloat(), centerX, centerY)
                    } else if (Surface.ROTATION_180 == rotation) {
                        matrix.postRotate(180f, centerX, centerY)
                    }
                    view.setTransform(matrix)
                }
            }
        }
    }

    internal class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int =
            signum((lhs?.run { width.toLong() * height }
                ?: 0L) - (rhs?.run { width.toLong() * height } ?: 0L))
    }

    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(activity!!)
                .setMessage(arguments?.getString(ARG_MESSAGE))
                .setPositiveButton(
                    R.string.ok
                ) { _, _ -> activity?.finish() }
                .create()
        }

        companion object {
            private const val ARG_MESSAGE = "MESSAGE"
            fun newInstance(message: String?): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }

    class ConfirmationDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val parent = parentFragment
            return AlertDialog.Builder(activity!!).setMessage(R.string.requestPermission)
                .setPositiveButton(R.string.ok) { _, _ ->
                    parent?.requestPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }.setNegativeButton(R.string.cancel) { _, _ ->
                    val activity = parent?.activity
                    activity?.finish()
                }.create()
        }
    }
}
