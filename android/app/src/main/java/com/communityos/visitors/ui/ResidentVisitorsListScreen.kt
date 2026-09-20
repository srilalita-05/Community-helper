package com.communityos.visitors.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.communityos.visitors.event.ResidentVisitorsEvent
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.state.VisitorFilter
import com.communityos.visitors.viewmodel.ResidentVisitorsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentVisitorsListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onVisitorClick: (String) -> Unit,
    viewModel: ResidentVisitorsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(ResidentVisitorsEvent.LoadVisitors)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Visitors",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = "Pre-Approve Visitor") },
                text = { Text("Pre-Approve") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filter == VisitorFilter.ALL,
                        onClick = { viewModel.onEvent(ResidentVisitorsEvent.SetFilter(VisitorFilter.ALL)) },
                        label = { Text("All (${state.visitors.size})") }
                    )
                }
                item {
                    val activeCount = state.visitors.count {
                        it.status == VisitorStatus.PRE_APPROVED || it.status == VisitorStatus.CHECKED_IN
                    }
                    FilterChip(
                        selected = state.filter == VisitorFilter.ACTIVE,
                        onClick = { viewModel.onEvent(ResidentVisitorsEvent.SetFilter(VisitorFilter.ACTIVE)) },
                        label = { Text("Active ($activeCount)") }
                    )
                }
                item {
                    val pastCount = state.visitors.count {
                        it.status == VisitorStatus.CHECKED_OUT ||
                        it.status == VisitorStatus.DENIED ||
                        it.status == VisitorStatus.CANCELLED
                    }
                    FilterChip(
                        selected = state.filter == VisitorFilter.PAST,
                        onClick = { viewModel.onEvent(ResidentVisitorsEvent.SetFilter(VisitorFilter.PAST)) },
                        label = { Text("Past ($pastCount)") }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.visitors.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null && state.visitors.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.error ?: "An unexpected error occurred",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.onEvent(ResidentVisitorsEvent.Refresh) }) {
                                Text("Retry")
                            }
                        }
                    }
                    state.filteredVisitors.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "No Visitors",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (state.visitors.isEmpty()) "No Visitors Yet" else "No Matching Visitors",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (state.visitors.isEmpty())
                                    "Pre-approve guests, delivery personnel, or cabs for quick and seamless gate entry."
                                else
                                    "No visitor passes found for the selected filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (state.visitors.isEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onNavigateToCreate,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Pre-Approve a Visitor")
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.filteredVisitors, key = { it.id }) { visitor ->
                                ResidentVisitorCard(
                                    visitor = visitor,
                                    onClick = { onVisitorClick(visitor.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResidentVisitorCard(
    visitor: Visitor,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(visitor.scheduledArrivalDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visitor.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                VisitorStatusBadge(status = visitor.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = visitor.purpose,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (!visitor.vehicleNumber.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = visitor.vehicleNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled: $formattedDate",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun VisitorStatusBadge(status: VisitorStatus) {
    val (containerColor, contentColor, label) = when (status) {
        VisitorStatus.PRE_APPROVED -> Triple(
            Color(0xFFFFECC8),
            Color(0xFFE65100),
            "Pre-Approved"
        )
        VisitorStatus.CHECKED_IN -> Triple(
            Color(0xFFC8E6C9),
            Color(0xFF2E7D32),
            "Checked In"
        )
        VisitorStatus.CHECKED_OUT -> Triple(
            Color(0xFFEEEEEE),
            Color(0xFF616161),
            "Checked Out"
        )
        VisitorStatus.DENIED -> Triple(
            Color(0xFFFFCDD2),
            Color(0xFFC62828),
            "Denied"
        )
        VisitorStatus.CANCELLED -> Triple(
            Color(0xFFE0E0E0),
            Color(0xFF757575),
            "Cancelled"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
