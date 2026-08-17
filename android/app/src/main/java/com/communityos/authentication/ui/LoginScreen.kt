package com.communityos.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.communityos.authentication.components.AuthButton
import com.communityos.authentication.components.AuthTextField
import com.communityos.authentication.event.AuthEvent
import com.communityos.authentication.state.AuthState

@Composable
fun LoginScreen(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToOtp: (String) -> Unit
) {
    LaunchedEffect(state.isOtpSent) {
        if (state.isOtpSent) {
            onNavigateToOtp(state.phoneNumber)
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
                text = "Join Your Community",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your phone number to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            AuthTextField(
                value = state.phoneNumber,
                onValueChange = { onEvent(AuthEvent.OnPhoneChanged(it)) },
                label = "Phone Number",
                placeholder = "+1 (555) 000-0000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
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
                text = "Send OTP",
                onClick = { onEvent(AuthEvent.SendOtp) },
                isLoading = state.isLoading,
                enabled = state.phoneNumber.isNotBlank()
            )
        }
    }
}
