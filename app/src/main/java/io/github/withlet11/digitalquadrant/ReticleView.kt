/*
 * ReticleView.kt
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
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.content.ContextCompat
import kotlin.math.truncate

@Composable
fun Reticle(altitude: Float, roll: Float, isPaused: Boolean, modifier: Modifier = Modifier) {
    val drawOutlineColor = Color(127, 95, 79)
    val drawColor = Color(255, 191, 159)

    val textMeasurer = rememberTextMeasurer()

    val vector =
        ImageVector.vectorResource(if (isPaused) R.drawable.ic_action_pause else R.drawable.ic_action_resume)
    val vectorPainter = rememberVectorPainter(image = vector)

    Box {
        MainCamera()
        Canvas(
            modifier = modifier
                .fillMaxSize()
        ) {
            val center = size.width / 2f
            val middle = size.height / 2f
            val reticleInside = size.height / 50f
            val reticleOutside = size.height * 7 / 50f
            val circleSize = size.height * 4 / 50f
            val normalTextSize = size.height / 25f
            val rect = Rect(Offset.Zero, size)

            rotate(-roll, rect.center) {
                for ((color, lineWidth) in arrayListOf(
                    Pair(drawOutlineColor, 10f),
                    Pair(drawColor, 5f)
                )) {
                    drawLine(
                        color = color,
                        start = Offset(center, middle - reticleOutside),
                        end = Offset(center, middle - reticleInside),
                        strokeWidth = lineWidth
                    )
                    drawLine(
                        color = color,
                        start = Offset(center, middle + reticleInside),
                        end = Offset(center, middle + reticleOutside),
                        strokeWidth = lineWidth
                    )
                    drawLine(
                        color = color,
                        start = Offset(center - reticleOutside, middle),
                        end = Offset(center - reticleInside, middle),
                        strokeWidth = lineWidth
                    )
                    drawLine(
                        color = color,
                        start = Offset(center + reticleInside, middle),
                        end = Offset(center + reticleOutside, middle),
                        strokeWidth = lineWidth
                    )
                    drawCircle(
                        color = color,
                        center = Offset(center, middle),
                        radius = circleSize,
                        style = Stroke(width = lineWidth)
                    )

                    // draw text of values
                    /*
                    paint.style =
                        if (color == drawColor) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                    paint.strokeWidth = lineWidth - 5f
                    for ((text, textSize) in arrayOf(
                        Pair("Roll: %5.0f°".format(roll), 1f),
                        Pair("Alt.: %6.1f°".format(altitude), 2f)
                    )) {
                        paint.textSize = normalTextSize * textSize
                        val textWidth = paint.measureText(text)
                        drawText(
                            text = text,
                            topLeft = Offset(
                                (size.width - textWidth) * 0.5f,
                                size.height * (6f + textSize) / 10f
                            )
                        )
                    }
                     */
                    for ((text, textSize) in arrayOf(
                        "Roll: %6.1f°".format(roll) to 1f,
                        "Alt: %6.1f°".format(altitude) to 2f
                    )) {
                        val style = if (color == drawColor) {
                            TextStyle(
                                fontSize = (normalTextSize * textSize).toSp(),
                                color = color,
                                background = Color.Transparent,
                            )
                        } else {
                            TextStyle(
                                // font size must be different from that of filled.
                                fontSize = (normalTextSize * textSize * 0.999f).toSp(),
                                color = color,
                                background = Color.Transparent,
                                drawStyle = Stroke(
                                    join = StrokeJoin.Round,
                                    width = 5f,
                                )
                            )
                        }

                        val measuredText =
                            textMeasurer.measure(AnnotatedString(text), style = style)
                        val textWidth = measuredText.size.width
                        val textHeight = measuredText.size.height

                        drawText(
                            textMeasurer = textMeasurer,
                            text = text,
                            topLeft = Offset(
                                (size.width - textWidth) * 0.5f,
                                size.height * (6f + textSize) / 10f - textHeight
                            ),
                            style = style
                        )
                    }
                }
            }

            // paint.textSize = normalTextSize

            // draw icon
            /*
            (if (isPaused) bmpResume!! else bmpPause!!).apply {
                setBounds(width * 2 / 5, height * 17 / 20, width * 3 / 5, height * 17 / 20 + width / 5)
                draw(canvas)
            }

            holder.unlockCanvasAndPost(canvas)
             */
            translate(left = size.width * 2 / 5, top = size.height * 17 / 20) {
                with(vectorPainter) {
                    draw(size = Size(size.width / 5, size.width / 5))
                }
            }
        }
    }
}

/*
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
}
 */
