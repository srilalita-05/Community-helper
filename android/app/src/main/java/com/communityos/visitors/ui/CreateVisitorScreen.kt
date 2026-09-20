package com.communityos.visitors.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.communityos.visitors.event.CreateVisitorEvent
import com.communityos.visitors.viewmodel.CreateVisitorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisitorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateVisitorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.onEvent(CreateVisitorEvent.ResetSuccess)
            onNavigateBack()
        }
    }

    val quickPurposes = listOf("Guest", "Delivery", "Cab / Taxi", "Service / Repair", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pre-Approve Visitor",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Text(
                text = "Visitor Information",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Security will verify this details at the gate before allowing entry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Visitor Name
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(CreateVisitorEvent.OnNameChanged(it)) },
                label = { Text("Visitor Full Name *") },
                placeholder = { Text("e.g. John Doe") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = { viewModel.onEvent(CreateVisitorEvent.OnPhoneChanged(it)) },
                label = { Text("Phone Number *") },
                placeholder = { Text("10-digit mobile number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                isError = state.phoneError != null,
                supportingText = state.phoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Purpose Selection
            Text(
                text = "Purpose of Visit *",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPurposes.take(3).forEach { p ->
                    FilterChip(
                        selected = state.purpose == p,
                        onClick = { viewModel.onEvent(CreateVisitorEvent.OnPurposeChanged(p)) },
                        label = { Text(p) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPurposes.drop(3).forEach { p ->
                    FilterChip(
                        selected = state.purpose == p,
                        onClick = { viewModel.onEvent(CreateVisitorEvent.OnPurposeChanged(p)) },
                        label = { Text(p) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.purpose,
                onValueChange = { viewModel.onEvent(CreateVisitorEvent.OnPurposeChanged(it)) },
                label = { Text("Custom Purpose / Notes") },
                isError = state.purposeError != null,
                supportingText = state.purposeError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vehicle Number (Optional)
            OutlinedTextField(
                value = state.vehicleNumber,
                onValueChange = { viewModel.onEvent(CreateVisitorEvent.OnVehicleChanged(it)) },
                label = { Text("Vehicle Number (Optional)") },
                placeholder = { Text("e.g. KA-05-MJ-1234") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scheduled Arrival Date display
            val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy · hh:mm a", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(state.scheduledArrivalDate))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scheduled Arrival",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (state.generalError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.generalError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.onEvent(CreateVisitorEvent.Submit) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Pre-Approve Visitor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
