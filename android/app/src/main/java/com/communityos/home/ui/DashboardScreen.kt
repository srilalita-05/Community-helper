package com.communityos.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.communityos.home.event.HomeEvent
import com.communityos.home.state.HomeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToNotices: () -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Feed", "Events", "Marketplace", "Profile")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.List,
        Icons.Default.DateRange,
        Icons.Default.ShoppingCart,
        Icons.Default.Person
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                0 -> HomeScreen(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToNotices = onNavigateToNotices
                )
                4 -> ProfileScreen(onLogout = { onEvent(HomeEvent.Logout) })
                else -> {
                    // Placeholders for Feed, Events, Marketplace
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${items[selectedItem]} Module",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This module will be fully integrated in later phases.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
