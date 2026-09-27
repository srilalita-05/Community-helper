package com.communityos.marketplace.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.communityos.marketplace.event.CreateEditListingEvent
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.viewmodel.CreateEditListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditListingScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateEditListingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            val msg = if (state.isEditMode) "Listing updated successfully" else "Listing posted successfully"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(CreateEditListingEvent.ResetSuccess)
            onNavigateBack()
        }
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val selectableCategories = remember {
        MarketplaceCategory.entries.filter { it != MarketplaceCategory.ALL }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Listing" else "Create Listing",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading && state.title.isBlank() && state.isEditMode) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.errorMessage?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(CreateEditListingEvent.OnTitleChanged(it)) },
                        label = { Text("Title *") },
                        placeholder = { Text("e.g. Wooden Dining Table, Study Lamp") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.titleError != null,
                        supportingText = state.titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.category.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            selectableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        viewModel.onEvent(CreateEditListingEvent.OnCategoryChanged(category))
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Price
                    OutlinedTextField(
                        value = state.priceText,
                        onValueChange = { viewModel.onEvent(CreateEditListingEvent.OnPriceChanged(it)) },
                        label = { Text("Price (₹) *") },
                        placeholder = { Text("e.g. 1500") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.priceError != null,
                        supportingText = state.priceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Contact Phone
                    OutlinedTextField(
                        value = state.contactPhone,
                        onValueChange = { viewModel.onEvent(CreateEditListingEvent.OnPhoneChanged(it)) },
                        label = { Text("Contact Phone *") },
                        placeholder = { Text("10-digit mobile number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = state.phoneError != null,
                        supportingText = state.phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Status Dropdown (only in Edit Mode)
                    if (state.isEditMode) {
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = state.status.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                ListingStatus.entries.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.name) },
                                        onClick = {
                                            viewModel.onEvent(CreateEditListingEvent.OnStatusChanged(status))
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Description
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged(it)) },
                        label = { Text("Description *") },
                        placeholder = { Text("Describe the condition, age, features, and pickup instructions...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        isError = state.descriptionError != null,
                        supportingText = state.descriptionError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.onEvent(CreateEditListingEvent.Submit) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (state.isEditMode) "Save Changes" else "Post Listing",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
