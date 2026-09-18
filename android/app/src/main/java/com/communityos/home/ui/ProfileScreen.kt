package com.communityos.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.communityos.models.UserRole
import com.communityos.profile.event.ProfileEvent
import com.communityos.profile.state.ProfileState
import com.communityos.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    ProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onLogout = onLogout
    )
}

@Composable
fun ProfileContent(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.isLoading && state.profile == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Success message banner
                if (state.successMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.successMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Error message banner
                if (state.errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
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
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                val roleText = when (state.profile?.role) {
                    UserRole.RESIDENT -> "Resident"
                    UserRole.ADMIN -> "Admin"
                    UserRole.SECURITY -> "Security"
                    null -> ""
                }

                if (state.isEditing) {
                    // Edit Mode
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Edit Profile",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = state.editName,
                                onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                                label = { Text("Name") },
                                isError = state.nameError != null,
                                supportingText = state.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ProfileFieldDisplay(
                                label = "Phone Number",
                                value = state.profile?.phoneNumber ?: "",
                                isReadOnly = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = state.editEmail,
                                onValueChange = { onEvent(ProfileEvent.EmailChanged(it)) },
                                label = { Text("Email") },
                                isError = state.emailError != null,
                                supportingText = state.emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ProfileFieldDisplay(
                                label = "Community",
                                value = state.profile?.communityName ?: "",
                                isReadOnly = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ProfileFieldDisplay(
                                label = "Flat",
                                value = state.profile?.flatNumber ?: "",
                                isReadOnly = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ProfileFieldDisplay(
                                label = "Role",
                                value = roleText,
                                isReadOnly = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEvent(ProfileEvent.CancelEditing) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            enabled = !state.isSaving
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { onEvent(ProfileEvent.SaveProfile) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                } else {
                    // View Mode
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ProfileFieldDisplay(
                                label = "Name",
                                value = state.profile?.name ?: ""
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ProfileFieldDisplay(
                                label = "Phone Number",
                                value = state.profile?.phoneNumber ?: ""
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ProfileFieldDisplay(
                                label = "Email",
                                value = state.profile?.email ?: ""
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ProfileFieldDisplay(
                                label = "Community",
                                value = state.profile?.communityName ?: ""
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ProfileFieldDisplay(
                                label = "Flat",
                                value = state.profile?.flatNumber ?: ""
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ProfileFieldDisplay(
                                label = "Role",
                                value = roleText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onEvent(ProfileEvent.StartEditing) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Log Out",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldDisplay(
    label: String,
    value: String,
    isReadOnly: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (isReadOnly) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(Read-only)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
