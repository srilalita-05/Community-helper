package com.communityos.maintenance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.communityos.maintenance.event.MaintenanceDashboardEvent
import com.communityos.maintenance.model.BillStatus
import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.state.MaintenanceDashboardState
import com.communityos.maintenance.state.MaintenanceTab
import com.communityos.maintenance.viewmodel.MaintenanceDashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBillDetails: (String) -> Unit,
    onNavigateToPayment: (String) -> Unit,
    viewModel: MaintenanceDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Maintenance & Dues",
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
                actions = {
                    IconButton(onClick = { viewModel.onEvent(MaintenanceDashboardEvent.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading && state.pendingBills.isEmpty() && state.billHistory.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Total Outstanding Balance Hero Card
                    OutstandingBalanceCard(
                        totalAmount = state.outstandingTotal,
                        pendingCount = state.pendingBills.size,
                        onPayNext = {
                            state.pendingBills.firstOrNull()?.let { onNavigateToPayment(it.id) }
                        }
                    )

                    // Error banner if any
                    state.error?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Navigation Tabs
                    PrimaryTabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = state.selectedTab == MaintenanceTab.PENDING,
                            onClick = {
                                viewModel.onEvent(MaintenanceDashboardEvent.SelectTab(MaintenanceTab.PENDING))
                            },
                            text = { Text("Due (${state.pendingBills.size})") }
                        )
                        Tab(
                            selected = state.selectedTab == MaintenanceTab.HISTORY,
                            onClick = {
                                viewModel.onEvent(MaintenanceDashboardEvent.SelectTab(MaintenanceTab.HISTORY))
                            },
                            text = { Text("History (${state.billHistory.size})") }
                        )
                        Tab(
                            selected = state.selectedTab == MaintenanceTab.PAYMENTS,
                            onClick = {
                                viewModel.onEvent(MaintenanceDashboardEvent.SelectTab(MaintenanceTab.PAYMENTS))
                            },
                            text = { Text("Payments (${state.payments.size})") }
                        )
                    }

                    // Content for Selected Tab
                    when (state.selectedTab) {
                        MaintenanceTab.PENDING -> {
                            if (state.pendingBills.isEmpty()) {
                                EmptyMaintenanceView(
                                    icon = Icons.Default.CheckCircle,
                                    title = "All dues cleared!",
                                    subtitle = "You have no outstanding or overdue maintenance bills."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.pendingBills, key = { it.id }) { bill ->
                                        MaintenanceBillCard(
                                            bill = bill,
                                            onCardClick = { onNavigateToBillDetails(bill.id) },
                                            onPayClick = { onNavigateToPayment(bill.id) }
                                        )
                                    }
                                }
                            }
                        }
                        MaintenanceTab.HISTORY -> {
                            if (state.billHistory.isEmpty()) {
                                EmptyMaintenanceView(
                                    icon = Icons.Default.Info,
                                    title = "No bill history",
                                    subtitle = "Past paid bills will appear here."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.billHistory, key = { it.id }) { bill ->
                                        MaintenanceBillCard(
                                            bill = bill,
                                            onCardClick = { onNavigateToBillDetails(bill.id) },
                                            onPayClick = null
                                        )
                                    }
                                }
                            }
                        }
                        MaintenanceTab.PAYMENTS -> {
                            if (state.payments.isEmpty()) {
                                EmptyMaintenanceView(
                                    icon = Icons.Default.Info,
                                    title = "No payment records",
                                    subtitle = "Your completed payments will be logged here."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.payments, key = { it.id }) { payment ->
                                        PaymentHistoryCard(payment = payment)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutstandingBalanceCard(
    totalAmount: Double,
    pendingCount: Int,
    onPayNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (totalAmount > 0.0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Total Outstanding Dues",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%,.2f", totalAmount)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = if (totalAmount > 0.0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (totalAmount > 0.0 && pendingCount > 0) {
                    Button(
                        onClick = onPayNext,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pay Now")
                    }
                }
            }
            if (pendingCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$pendingCount pending bill${if (pendingCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MaintenanceBillCard(
    bill: MaintenanceBill,
    onCardClick: () -> Unit,
    onPayClick: (() -> Unit)?
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Period: ${bill.period}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BillStatusBadge(status = bill.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Due: ${dateFormat.format(Date(bill.dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bill.status == BillStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", bill.amount)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (onPayClick != null && bill.isPayable) {
                    FilledTonalButton(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pay Now")
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BillStatusBadge(status: BillStatus) {
    val (bgColor, textColor, label) = when (status) {
        BillStatus.PAID -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "PAID")
        BillStatus.UNPAID -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "DUE")
        BillStatus.OVERDUE -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "OVERDUE")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
private fun PaymentHistoryCard(payment: MaintenancePayment) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ref: ${payment.transactionRef}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = payment.paymentMethod.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(payment.paymentDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${String.format(Locale.getDefault(), "%,.2f", payment.amountPaid)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun EmptyMaintenanceView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
