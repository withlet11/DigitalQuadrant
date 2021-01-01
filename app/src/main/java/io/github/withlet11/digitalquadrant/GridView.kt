/*
 * GridView.kt
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min


class GridView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint()

    private val backgroundColor = Color.rgb(15, 15, 15)
    private val meshColor = Color.rgb(63, 63, 63)
    private val squareColor = Color.rgb(191, 255, 191)
    private val textColor = Color.rgb(255, 191, 159)
    private val iconPause = R.drawable.ic_action_pause
    private val iconResume = R.drawable.ic_action_resume
    private var bmpPause: Drawable? = null
    private var bmpResume: Drawable? = null

    private var pitch = 0.0f
    private var roll = 0.0f

    var isPaused = false
        private set

    override fun onDraw(canvas: Canvas) {
        if (bmpPause == null) bmpPause = ContextCompat.getDrawable(context!!, iconPause)!!
        if (bmpResume == null) bmpResume = ContextCompat.getDrawable(context!!, iconResume)!!

        val left = 0.0f
        val center = width * 0.5f
        val right = width.toFloat()
        val top = 0.0f
        val middle = height * 0.5f
        val bottom = height.toFloat()
        val squareSize = min(width, height) / 50.0f
        val pitchInterval = height / 180.0f
        val rollInterval = width / 180.0f
        val normalTextSize = height / 50.0f

        // paint background
        canvas.drawColor(backgroundColor)

        // common settings
        paint.style = Paint.Style.FILL_AND_STROKE

        // draw mesh
        paint.color = meshColor
        canvas.drawRect(center - squareSize, top, center + squareSize, bottom, paint)
        paint.strokeWidth = 8.0f
        canvas.drawLine(left, middle, right, middle, paint)
        paint.strokeWidth = 1.0f
        paint.textSize = normalTextSize

        for (i in 0..18) {
            canvas.drawLine(
                left,
                i * pitchInterval * 10.0f,
                right,
                i * pitchInterval * 10.0f,
                paint
            )
            canvas.drawLine(i * rollInterval * 10.0f, top, i * rollInterval * 10.0f, bottom, paint)
            canvas.drawText(
                "%+2d°".format(90 - i * 10),
                center + rollInterval * 10.0f,
                pitchInterval * i * 10.0f,
                paint
            )

        }

        // draw square
        paint.color = squareColor
        canvas.drawRect(
            center + min(roll * rollInterval, -squareSize),
            middle - pitch * pitchInterval - squareSize,
            center + max(roll * rollInterval, squareSize),
            middle - pitch * pitchInterval + squareSize,
            paint
        )

        // draw text of values
        paint.color = textColor
        for ((text, textSize) in arrayOf("Roll: %6.1f°".format(roll) to 2f,
                    "Pitch: %6.1f°".format(pitch) to 4f)) {
            paint.textSize = normalTextSize * textSize
            val textWidth = paint.measureText(text)
            canvas.drawText(text, (width - textWidth) * 0.5f, height * (0.6f + textSize / 20f), paint)
        }

        // draw icon
        (if (isPaused) bmpResume!! else bmpPause!!).apply {
            setBounds(width * 2 / 5, height * 17 / 20, width * 3 / 5, height * 17 / 20 + width / 5)
            draw(canvas)
        }
    }

    fun setPosition(pitch: Float, roll: Float) {
        if (!isPaused) {
            this.pitch = pitch
            this.roll = roll
        }

        invalidate()
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun pause() {
        isPaused = true
        invalidate()
    }
}
