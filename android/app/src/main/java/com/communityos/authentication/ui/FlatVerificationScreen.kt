package com.communityos.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.communityos.authentication.components.AuthButton
import com.communityos.authentication.components.AuthTextField
import com.communityos.authentication.event.AuthEvent
import com.communityos.authentication.state.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatVerificationScreen(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateNext: (role: String) -> Unit
) {
    LaunchedEffect(state.isVerificationSubmitted) {
        if (state.isVerificationSubmitted) {
            onNavigateNext(state.selectedRole)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Flat Verification",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = state.flatNo,
                onValueChange = { onEvent(AuthEvent.OnFlatNoChanged(it)) },
                label = "Flat / Unit Number (e.g. C-402)",
                placeholder = "C-402"
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Select Your Role",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Resident", "Admin", "Security").forEach { role ->
                    ElevatedFilterChip(
                        selected = state.selectedRole == role,
                        onClick = { onEvent(AuthEvent.OnRoleSelected(role)) },
                        label = { Text(role) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            AuthButton(
                text = "Request Verification",
                onClick = { onEvent(AuthEvent.SubmitFlatVerification) },
                isLoading = state.isLoading,
                enabled = state.flatNo.isNotBlank()
            )
        }
    }
}
