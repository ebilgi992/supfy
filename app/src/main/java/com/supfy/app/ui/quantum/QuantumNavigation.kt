package com.supfy.app.ui.quantum

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class QuantumDestination(val label: String) {
    HOME("Ana sayfa"), SEARCH("Ara"), LIBRARY("Kitaplığın"), SETTINGS("Ayarlar"),
}

@Composable
fun QuantumBottomBar(selected: QuantumDestination, onSelect: (QuantumDestination) -> Unit) {
    NavigationBar(containerColor = QuantumInk, contentColor = QuantumMuted, tonalElevation = 0.dp) {
        QuantumDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(when (destination) {
                        QuantumDestination.HOME -> Icons.Filled.Home
                        QuantumDestination.SEARCH -> Icons.Filled.Search
                        QuantumDestination.LIBRARY -> QuantumIcons.Library
                        QuantumDestination.SETTINGS -> Icons.Filled.Settings
                    }, contentDescription = null)
                },
                label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = QuantumGreen,
                    selectedTextColor = QuantumWhite,
                    indicatorColor = QuantumGreen.copy(alpha = .11f),
                    unselectedIconColor = QuantumMuted,
                    unselectedTextColor = QuantumMuted,
                ),
            )
        }
    }
}
