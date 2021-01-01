/**
 * ReticleView.kt
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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import kotlin.math.truncate

class ReticleView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val paint = Paint()

    private val drawOutlineColor = Color.rgb(127, 95, 79)
    private val drawColor = Color.rgb(255, 191, 159)
    private val iconPause = R.drawable.ic_action_pause
    private val iconResume = R.drawable.ic_action_resume
    private var bmpPause: Drawable? = null
    private var bmpResume: Drawable? = null

    private var altitude = 0f
    private var roll = 0f

    var isPaused = false
        private set

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        drawView()

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    private fun drawView() {
        val canvas = holder.lockCanvas()

        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        if (bmpPause == null) bmpPause = ContextCompat.getDrawable(context!!, iconPause)!!
        if (bmpResume == null) bmpResume = ContextCompat.getDrawable(context!!, iconResume)!!

        val center = width / 2f
        val middle = height / 2f
        val reticleInside = height / 50f
        val reticleOutside = height * 7 / 50f
        val circleSize = height * 4 / 50f
        val normalTextSize = height / 25f

        canvas.rotate(-roll, center, middle)
        for ((color, lineWidth) in arrayListOf(Pair(drawOutlineColor, 10f), Pair(drawColor, 5f))) {
            // draw reticle
            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = lineWidth
            canvas.drawLine(center, middle - reticleOutside, center, middle - reticleInside , paint)
            canvas.drawLine(center, middle + reticleInside, center, middle + reticleOutside, paint)
            canvas.drawLine(center - reticleOutside, middle, center - reticleInside , middle, paint)
            canvas.drawLine(center + reticleInside, middle, center + reticleOutside, middle, paint)
            canvas.drawCircle(center, middle, circleSize, paint)

            // draw text of values
            paint.style = if (color == drawColor) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
            paint.strokeWidth = lineWidth - 5f
            for ((text, textSize) in arrayOf(Pair("Roll: %5.0f°".format(roll), 1f),
                Pair("Alt.: %6.1f°".format(altitude), 2f))) {
                paint.textSize = normalTextSize * textSize
                val textWidth = paint.measureText(text)
                canvas.drawText(text, (width - textWidth) * 0.5f , height * (6f + textSize) / 10f, paint)
            }
        }

        // paint.textSize = normalTextSize

        // draw icon
        (if (isPaused) bmpResume!! else bmpPause!!).apply {
            setBounds(width * 2 / 5, height * 17 / 20, width * 3 / 5, height * 17 / 20 + width / 5)
            draw(canvas)
        }

        holder.unlockCanvasAndPost(canvas)
    }

    fun setPosition(altitude: Float, roll: Float) {
        if (!isPaused) {
            this.altitude = -altitude
            this.roll = truncate(-roll)
        }

        drawView()
    }

    fun togglePause() {
        isPaused = !isPaused
        invalidate()
    }

    fun pause() {
        isPaused = true
        invalidate()
    }
}
