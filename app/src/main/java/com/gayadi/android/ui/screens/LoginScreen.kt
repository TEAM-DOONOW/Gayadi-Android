package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.R
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily

@Composable
fun LoginScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(44.dp))

        Image(
            painter = painterResource(R.drawable.gayadi_text),
            contentDescription = "Gayadi",
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(160.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(30.dp))

        Image(
            painter = painterResource(R.drawable.ganadi),
            contentDescription = "가야디 캐릭터",
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.weight(0.5f))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "시작하기",
                fontSize = 16.sp,
                fontFamily = PretendardSemiBoldFontFamily,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    GayadiTheme { LoginScreen(onStart = {}) }
}
