package com.gayadi.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Background,
    secondary = KakaoYellow,
    onSecondary = KakaoText,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Divider,
    error = TagRedText,
    onError = Background,
)

@Composable
fun GayadiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = GayadiTypography,
        content = content,
    )
}
