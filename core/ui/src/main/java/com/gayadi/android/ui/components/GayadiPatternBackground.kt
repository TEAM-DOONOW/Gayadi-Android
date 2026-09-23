package com.gayadi.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gayadi.android.core.ui.R
import com.gayadi.android.ui.theme.Background
import com.gayadi.android.ui.theme.PatternBackground

/** Shared decorative page background; content and cards are drawn above it. */
@Composable
fun Modifier.gayadiPatternBackground(topWave: Boolean = false, waveHeight: Dp = 330.dp): Modifier {
    val image = ImageBitmap.imageResource(R.drawable.gayadi_background)
    return background(Background).drawWithCache {
        val patternHeight = if (topWave) minOf(waveHeight.toPx(), size.height * 0.65f) else size.height
        val waveDepth = if (topWave) 24.dp.toPx() else 0f
        val boundary = patternHeight - waveDepth
        val outline = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, boundary)
            if (topWave) {
                cubicTo(
                    size.width * 0.83f, boundary - waveDepth,
                    size.width * 0.67f, boundary - waveDepth,
                    size.width * 0.5f, boundary,
                )
                cubicTo(
                    size.width * 0.33f, boundary + waveDepth,
                    size.width * 0.17f, boundary + waveDepth,
                    0f, boundary,
                )
            } else {
                lineTo(0f, boundary)
            }
            close()
        }
        val tileSize = 460.dp.roundToPx()
        val columns = (size.width / tileSize).toInt() + 1
        val rows = (patternHeight / tileSize).toInt() + 1
        onDrawBehind {
            clipPath(outline) {
                drawRect(PatternBackground)
                repeat(rows) { row ->
                    repeat(columns) { column ->
                        drawImage(
                            image = image,
                            dstSize = IntSize(tileSize, tileSize),
                            dstOffset = IntOffset(column * tileSize, row * tileSize),
                            alpha = 0.32f,
                        )
                    }
                }
            }
        }
    }
}
