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
import com.communityos.authentication.components.OtpInputField
import com.communityos.authentication.event.AuthEvent
import com.communityos.authentication.state.AuthState

@Composable
fun OtpVerificationScreen(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateNext: (isNewUser: Boolean) -> Unit
) {
    LaunchedEffect(state.currentUser) {
        state.currentUser?.let { user ->
            onNavigateNext(user.isNewUser)
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
                text = "Verify Code",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We sent an OTP to ${state.phoneNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            OtpInputField(
                otpLength = 6,
                onOtpChange = { onEvent(AuthEvent.OnOtpChanged(it)) }
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
                text = "Verify",
                onClick = { onEvent(AuthEvent.VerifyOtp) },
                isLoading = state.isLoading,
                enabled = state.otpCode.length == 6
            )
        }
    }
}
