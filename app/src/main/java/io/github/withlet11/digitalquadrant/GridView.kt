/*
 * GridView.kt
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.max
import kotlin.math.min

@Composable
fun GridView(pitch: Float, roll: Float, isPaused: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = Color(15, 15, 15)
    val meshColor = Color(63, 63, 63)
    val squareColor = Color(191, 255, 191)
    val textColor = Color(255, 191, 159)
    val textMeasurer = rememberTextMeasurer()
    val vector =
        ImageVector.vectorResource(if (isPaused) R.drawable.ic_action_pause else R.drawable.ic_action_resume)
    val vectorPainter = rememberVectorPainter(image = vector)

    Canvas(modifier = modifier
        .fillMaxSize()
        .drawBehind { drawRect(backgroundColor) }) {
        val left = 0f
        val center = size.width * 0.5f
        val right = size.width
        val top = 0f
        val middle = size.height * 0.5f
        val bottom = size.height
        val squareSize = min(size.width, size.height) / 50f
        val pitchInterval = size.height / 180f
        val rollInterval = size.width / 180f
        val normalTextSize = size.height / 50f

        // Draws grids.
        drawRect(
            color = meshColor,
            topLeft = Offset(center - squareSize, top),
            size = Size(squareSize * 2, bottom - top)
        )

        drawLine(
            color = meshColor,
            start = Offset(left, middle),
            end = Offset(right, middle),
            strokeWidth = 8f
        )

        for (i in 0..18) {
            drawLine(
                color = meshColor,
                start = Offset(left, i * pitchInterval * 10f),
                end = Offset(right, i * pitchInterval * 10f),
                strokeWidth = 1f
            )
            drawLine(
                color = meshColor,
                start = Offset(i * rollInterval * 10f, top),
                end = Offset(i * rollInterval * 10f, bottom),
                strokeWidth = 1f
            )

            val style = TextStyle(
                color = meshColor,
                fontSize = normalTextSize.toSp(),
                background = Color.Transparent
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "%+2d°".format(90 - i * 10),
                topLeft = Offset(
                    center + rollInterval * 10f,
                    pitchInterval * i * 10f - normalTextSize
                ),
                style = style
            )
        }

        // Draws square.
        val squareLeftEdge = center + min(roll * rollInterval, -squareSize)
        val squareRightEdge = center + max(roll * rollInterval, squareSize)

        drawRect(
            color = squareColor,
            topLeft = Offset(squareLeftEdge, middle - pitch * pitchInterval - squareSize),
            size = Size(squareRightEdge - squareLeftEdge, squareSize * 2),
        )

        // Draws text.
        for ((text, textSize) in arrayOf(
            "Roll: %6.1f°".format(roll) to 2f,
            "Pitch: %6.1f°".format(pitch) to 4f
        )) {
            val style = TextStyle(
                fontSize = (normalTextSize * textSize).toSp(),
                color = textColor,
                background = Color.Transparent
            )
            val measuredText =
                textMeasurer.measure(
                    AnnotatedString(text),
                    style = style
                )
            val textWidth = measuredText.size.width
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(
                    center - textWidth * 0.5f, size.height * (0.6f + textSize / 37f),
                ),
                style = style
            )
        }

        // Draws icon.
        translate(left = size.width * 2 / 5, top = size.height * 17 / 20) {
            with(vectorPainter) {
                draw(size = Size(size.width / 5, size.width / 5))
            }
        }
    }
}