package com.gayadi.android

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gayadi.android.navigation.GayadiNavHost
import com.gayadi.android.di.AppContainer
import com.gayadi.android.ui.theme.GayadiTheme
import com.kakao.sdk.common.KakaoSdk
import java.io.File

class MainActivity : ComponentActivity() {
    private val appContainer by lazy {
        AppContainer(
            travelFile = File(filesDir, "travel-state.json"),
            tokenFile = File(filesDir, "auth-token"),
            apiBaseUrl = BuildConfig.API_BASE_URL,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.KAKAO_NATIVE_SDK.isNotBlank()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_SDK)
        }
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            GayadiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GayadiNavHost(appContainer = appContainer)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 45
    }
}
