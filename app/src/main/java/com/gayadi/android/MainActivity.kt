package com.gayadi.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gayadi.android.navigation.GayadiNavHost
import com.gayadi.android.di.AppContainer
import com.gayadi.android.ui.theme.GayadiTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val appContainer by lazy {
        AppContainer(
            profileFile = File(filesDir, "user-profile.xml"),
            travelFile = File(filesDir, "travel-state.json"),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GayadiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GayadiNavHost(appContainer = appContainer)
                }
            }
        }
    }
}
