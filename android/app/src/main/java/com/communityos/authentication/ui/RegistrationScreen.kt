package com.communityos.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.communityos.authentication.components.AuthButton
import com.communityos.authentication.components.AuthTextField
import com.communityos.authentication.event.AuthEvent
import com.communityos.authentication.state.AuthState

@Composable
fun RegistrationScreen(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateNext: () -> Unit
) {
    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) {
            onNavigateNext()
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
                text = "Create Profile",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Provide your name and email to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            AuthTextField(
                value = state.name,
                onValueChange = { onEvent(AuthEvent.OnNameChanged(it)) },
                label = "Full Name",
                placeholder = "John Doe"
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = state.email,
                onValueChange = { onEvent(AuthEvent.OnEmailChanged(it)) },
                label = "Email Address",
                placeholder = "john.doe@example.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            AuthButton(
                text = "Register",
                onClick = { onEvent(AuthEvent.RegisterUser) },
                isLoading = state.isLoading,
                enabled = state.name.isNotBlank() && state.email.isNotBlank()
            )
        }
    }
}
