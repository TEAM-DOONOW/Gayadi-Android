package com.gayadi.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextSecondary

enum class BottomTab(val label: String) {
    OUR_TRIP("우리여행"),
    MY_TRIP("나의여행"),
    MY_PAGE("마이"),
}

@Composable
fun BottomNavBar(
    currentTab: BottomTab,
    showMyPage: Boolean = false,
    onTabSelected: (BottomTab) -> Unit,
) {
    val rightTab = if (showMyPage) BottomTab.MY_PAGE else BottomTab.MY_TRIP

    NavigationBar(
        containerColor = Color.White,
    ) {
        NavigationBarItem(
            selected = currentTab == BottomTab.OUR_TRIP,
            onClick = { onTabSelected(BottomTab.OUR_TRIP) },
            icon = {
                Icon(
                    imageVector = if (currentTab == BottomTab.OUR_TRIP) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = BottomTab.OUR_TRIP.label,
                )
            },
            label = { Text(BottomTab.OUR_TRIP.label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
        NavigationBarItem(
            selected = currentTab == rightTab,
            onClick = { onTabSelected(rightTab) },
            icon = {
                Icon(
                    imageVector = if (currentTab == rightTab) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = rightTab.label,
                )
            },
            label = { Text(rightTab.label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
    }
}
