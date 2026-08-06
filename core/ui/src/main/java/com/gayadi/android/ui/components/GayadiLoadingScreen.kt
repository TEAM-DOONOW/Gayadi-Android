package com.gayadi.android.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.core.ui.R
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private const val MinimumLoadingDurationMillis = 2_000L

@Composable
fun rememberMinimumLoadingVisibility(isLoading: Boolean): Boolean {
    var showLoading by remember { mutableStateOf(isLoading) }
    var loadingStartedAt by remember {
        mutableLongStateOf(if (isLoading) SystemClock.elapsedRealtime() else 0L)
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            loadingStartedAt = SystemClock.elapsedRealtime()
            showLoading = true
        } else if (showLoading) {
            val elapsed = SystemClock.elapsedRealtime() - loadingStartedAt
            delay((MinimumLoadingDurationMillis - elapsed).coerceAtLeast(0L))
            showLoading = false
        }
    }

    return showLoading
}

@Composable
fun GayadiLoadingScreen(modifier: Modifier = Modifier) {
    var visibleDotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            visibleDotCount = (visibleDotCount + 1) % 4
        }
    }

    val planeMotion = rememberInfiniteTransition(label = "loadingPlane")
    val planeOffset by planeMotion.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loadingPlaneOffset",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFCFE9FF), Color(0xFFF4FAFF)),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LoadingCloud(
            modifier = Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = 52.dp),
            size = 120,
            alpha = 0.88f,
        )
        LoadingCloud(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = 150.dp),
            size = 92,
            alpha = 0.72f,
        )
        LoadingCloud(
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-28).dp, y = (-100).dp),
            size = 150,
            alpha = 0.64f,
        )
        LoadingCloud(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = (-30).dp),
            size = 110,
            alpha = 0.8f,
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.loading_ganadi),
                contentDescription = "비행기를 타고 이동하는 가야디",
                modifier = Modifier.size(270.dp).offset(y = planeOffset.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.width(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading${" .".repeat(visibleDotCount)}",
                    fontSize = 18.sp,
                    fontFamily = PretendardFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun LoadingCloud(modifier: Modifier, size: Int, alpha: Float) {
    Icon(
        imageVector = Icons.Rounded.Cloud,
        contentDescription = null,
        modifier = modifier.size(size.dp),
        tint = Color.White.copy(alpha = alpha),
    )
}

@Preview(showBackground = true)
@Composable
private fun GayadiLoadingScreenPreview() {
    GayadiTheme {
        GayadiLoadingScreen()
    }
}
