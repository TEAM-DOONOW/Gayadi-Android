package com.gayadi.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PrimaryBlue
import kotlin.math.roundToInt

enum class UsageGuidePlacement { ABOVE, BELOW }

data class UsageGuideCallout(
    val target: Rect,
    val text: AnnotatedString,
    val placement: UsageGuidePlacement,
    val spotlightPadding: Dp = 8.dp,
    val spotlightRadius: Dp = 14.dp,
)

@Composable
fun UsageGuideOverlay(
    callouts: List<UsageGuideCallout>,
    onDismiss: () -> Unit,
    onTargetClick: ((Int) -> Unit)? = null,
) {
    if (callouts.isEmpty()) return

    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val transition = rememberInfiniteTransition(label = "usage-guide")
    val arrowMotion by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "usage-guide-arrow",
    )
    val spotlightAlpha by transition.animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "usage-guide-spotlight",
    )

    Box(modifier = Modifier.fillMaxSize().zIndex(20f)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.76f))
            callouts.forEach { callout ->
                val padding = callout.spotlightPadding.toPx()
                val topLeft = Offset(callout.target.left - padding, callout.target.top - padding)
                val size = Size(callout.target.width + padding * 2, callout.target.height + padding * 2)
                val radius = CornerRadius(callout.spotlightRadius.toPx())
                drawRoundRect(Color.Transparent, topLeft, size, radius, blendMode = BlendMode.Clear)
                drawRoundRect(
                    Color.White.copy(alpha = spotlightAlpha),
                    topLeft,
                    size,
                    radius,
                    style = Stroke(2.dp.toPx()),
                )
            }
        }

        if (onTargetClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = interactionSource, indication = null) {},
            )
            callouts.forEachIndexed { index, callout ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(callout.target.left.roundToInt(), callout.target.top.roundToInt())
                        }
                        .size(
                            with(density) { callout.target.width.toDp() },
                            with(density) { callout.target.height.toDp() },
                        )
                        .clickable(
                            interactionSource = remember(index) { MutableInteractionSource() },
                            indication = null,
                        ) { onTargetClick(index) },
                )
            }
        }

        callouts.forEach { callout ->
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    Text(
                        text = callout.text,
                        fontFamily = PretendardFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 19.sp,
                        lineHeight = 27.sp,
                        letterSpacing = (-0.2).sp,
                        color = Color.White.copy(alpha = 0.96f),
                        textAlign = TextAlign.Center,
                    )
                    Icon(
                        imageVector = if (callout.placement == UsageGuidePlacement.ABOVE) {
                            Icons.Filled.ArrowDownward
                        } else {
                            Icons.Filled.ArrowUpward
                        },
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(44.dp),
                    )
                },
            ) { measurables, constraints ->
                val horizontalPadding = 32.dp.roundToPx()
                val textPlaceable = measurables[0].measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxWidth = (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0),
                    ),
                )
                val arrowPlaceable = measurables[1].measure(
                    constraints.copy(minWidth = 0, minHeight = 0),
                )
                val targetGap = 12.dp.roundToPx()
                val contentGap = 6.dp.roundToPx()
                val groupHeight = textPlaceable.height + contentGap + arrowPlaceable.height
                val groupTop = when (callout.placement) {
                    UsageGuidePlacement.ABOVE ->
                        (callout.target.top.roundToInt() - targetGap - groupHeight).coerceAtLeast(0)
                    UsageGuidePlacement.BELOW ->
                        (callout.target.bottom.roundToInt() + targetGap)
                            .coerceAtMost((constraints.maxHeight - groupHeight).coerceAtLeast(0))
                }
                val textX = (constraints.maxWidth - textPlaceable.width) / 2
                val arrowX = (callout.target.center.x.roundToInt() - arrowPlaceable.width / 2)
                    .coerceIn(0, (constraints.maxWidth - arrowPlaceable.width).coerceAtLeast(0))
                val motionY = arrowMotion.dp.roundToPx()

                layout(constraints.maxWidth, constraints.maxHeight) {
                    if (callout.placement == UsageGuidePlacement.ABOVE) {
                        textPlaceable.placeRelative(textX, groupTop)
                        arrowPlaceable.placeRelative(
                            arrowX,
                            groupTop + textPlaceable.height + contentGap + motionY,
                        )
                    } else {
                        arrowPlaceable.placeRelative(arrowX, groupTop + motionY)
                        textPlaceable.placeRelative(
                            textX,
                            groupTop + arrowPlaceable.height + contentGap,
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "사용 안내 닫기",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
