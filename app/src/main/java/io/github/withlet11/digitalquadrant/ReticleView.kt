/*
 * ReticleView.kt
 *
 * Copyright 2020-2025 Yasuhiro Yamakawa <withlet11@gmail.com>
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

@Composable
fun ReticleView(altitude: Float, roll: Float, isPaused: Boolean, modifier: Modifier = Modifier) {
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

            translate(left = size.width * 2 / 5, top = size.height * 17 / 20) {
                with(vectorPainter) {
                    draw(size = Size(size.width / 5, size.width / 5))
                }
            }
        }
    }
}
