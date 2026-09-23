package com.gayadi.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.ui.theme.*

@Composable
fun TransportModeSelector(selected: RouteTransportMode?, onSelect: (RouteTransportMode?) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(RouteTransportMode.CAR, RouteTransportMode.PUBLIC_TRANSIT, RouteTransportMode.WALK, RouteTransportMode.BICYCLE).forEach { mode ->
            val active = selected == mode
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (active) PrimaryAction else Background,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                border = if (active) null else BorderStroke(1.dp, Border),
            ) {
                Column(
                    Modifier.selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(if (active) null else mode) })
                        .heightIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val icon = when (mode) {
                        RouteTransportMode.CAR -> Icons.Default.DirectionsCar
                        RouteTransportMode.PUBLIC_TRANSIT -> Icons.Default.DirectionsBus
                        RouteTransportMode.WALK -> Icons.Default.DirectionsWalk
                        RouteTransportMode.BICYCLE -> Icons.Default.DirectionsBike
                    }
                    val label = when (mode) {
                        RouteTransportMode.CAR -> "자동차"
                        RouteTransportMode.PUBLIC_TRANSIT -> "버스"
                        RouteTransportMode.WALK -> "도보"
                        RouteTransportMode.BICYCLE -> "자전거"
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
