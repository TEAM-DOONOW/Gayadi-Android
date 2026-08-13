package com.gayadi.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gayadi.android.core.ui.R
import com.gayadi.android.ui.theme.TextSecondary

private val BottomNavSelectedColor = Color(0xFF343548)

enum class BottomTab(val label: String) {
    OUR_TRIP("우리여행"),
    LEDGER("가계부"),
    MY_TRIP("나의여행"),
    MY_PAGE("마이"),
}

@Composable
fun BottomNavBar(
    currentTab: BottomTab,
    showMyPage: Boolean = false,
    showLedger: Boolean = false,
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
                    painter = painterResource(R.drawable.bottom_our_trip),
                    contentDescription = BottomTab.OUR_TRIP.label,
                    modifier = Modifier.size(28.dp),
                )
            },
            label = { Text(BottomTab.OUR_TRIP.label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavSelectedColor,
                selectedTextColor = BottomNavSelectedColor,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
        if (showLedger) {
            NavigationBarItem(
                selected = currentTab == BottomTab.LEDGER,
                onClick = { onTabSelected(BottomTab.LEDGER) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.bottom_my_money),
                        contentDescription = BottomTab.LEDGER.label,
                        modifier = Modifier.size(28.dp),
                    )
                },
                label = { Text(BottomTab.LEDGER.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BottomNavSelectedColor,
                    selectedTextColor = BottomNavSelectedColor,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
        NavigationBarItem(
            selected = currentTab == rightTab,
            onClick = { onTabSelected(rightTab) },
            icon = {
                Icon(
                    painter = painterResource(
                        if (rightTab == BottomTab.MY_PAGE) {
                            R.drawable.bottom_my_page
                        } else {
                            R.drawable.bottom_my_trip
                        },
                    ),
                    contentDescription = rightTab.label,
                    modifier = Modifier.size(28.dp),
                )
            },
            label = { Text(rightTab.label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavSelectedColor,
                selectedTextColor = BottomNavSelectedColor,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
    }
}
