package com.gayadi.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.gayadi.android.core.ui.R
import com.gayadi.android.ui.theme.SurfaceLight

@Composable
fun PlacePhoto(imageUrl: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var loaded by remember(imageUrl) { mutableStateOf(false) }
    Box(modifier.background(SurfaceLight), contentAlignment = Alignment.Center) {
        if (!loaded) {
            Image(
                painter = painterResource(R.drawable.gayadi_trip),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f),
                contentScale = ContentScale.Fit,
            )
        }
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { loaded = false },
                onSuccess = { loaded = true },
                onError = { loaded = false },
            )
        }
    }
}
