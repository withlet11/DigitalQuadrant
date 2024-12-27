/*
 * CameralessQuadrantFragment.kt
 *
 * Copyright 2020-2024 Yasuhiro Yamakawa <withlet11@gmail.com>
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

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.hardware.Sensor
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class CameralessQuadrantFragment : QuadrantFragment() {
    private lateinit var gridView: GridView
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private lateinit var runnable: Runnable
    private lateinit var vibrator: Vibrator
    private lateinit var vibratorManager: VibratorManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_cameraless_quadrant, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gridView = view.findViewById(R.id.canvas)
        gridView.setOnClickListener { gridView.togglePause() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager =
                requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = requireContext().getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onResume() {
        super.onResume()
        timerSet()
    }

    override fun onPause() {
        stopTimerTask()
        super.onPause()
    }

    private fun timerSet() {
        runnable = object : Runnable {
            override fun run() {
                if (isStable) {
                    if (!gridView.isPaused) {
                        gridView.pause()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibrationEffect =
                                VibrationEffect.createOneShot(
                                    300,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            vibrator.vibrate(vibrationEffect)
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(300)
                        }
                    }
                } else {
                    gridView.setPosition(pitchY, rollY)
                }
                handler.postDelayed(this, PERIOD)
            }
        }
        handler.post(runnable)
    }

    private fun stopTimerTask() {
        handler.removeCallbacks(runnable)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
}