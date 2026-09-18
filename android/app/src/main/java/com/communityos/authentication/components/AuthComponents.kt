package com.communityos.authentication.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpInputField(
    otpLength: Int = 6,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpValue: String = ""
) {
    var codeState by remember {
        mutableStateOf(
            if (otpValue.isNotEmpty()) {
                List(otpLength) { index -> otpValue.getOrNull(index)?.toString() ?: "" }
            } else {
                List(otpLength) { "" }
            }
        )
    }

    val focusRequesters = remember { List(otpLength) { FocusRequester() } }

    // Auto-focus first empty block on launch
    LaunchedEffect(Unit) {
        val firstEmpty = codeState.indexOfFirst { it.isEmpty() }.let { if (it == -1) 0 else it }
        focusRequesters.getOrNull(firstEmpty)?.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until otpLength) {
            OutlinedTextField(
                value = codeState[i],
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    when {
                        // Multi-digit paste (e.g. pasting "123456")
                        filtered.length > 1 && input.length != 2 -> {
                            val newCode = codeState.toMutableList()
                            val chars = filtered.take(otpLength)
                            for (j in chars.indices) {
                                newCode[j] = chars[j].toString()
                            }
                            codeState = newCode
                            onOtpChange(newCode.joinToString(""))
                            val nextFocus = (chars.length).coerceAtMost(otpLength - 1)
                            focusRequesters.getOrNull(nextFocus)?.requestFocus()
                        }
                        // Replacing an existing single character in the box (e.g. typing "2" when box had "1")
                        filtered.length == 2 -> {
                            val newChar = if (filtered[0].toString() == codeState[i]) filtered[1] else filtered[0]
                            val newCode = codeState.toMutableList()
                            newCode[i] = newChar.toString()
                            codeState = newCode
                            onOtpChange(newCode.joinToString(""))
                            if (i < otpLength - 1) {
                                focusRequesters[i + 1].requestFocus()
                            }
                        }
                        // Entering a single digit into an empty box
                        filtered.length == 1 -> {
                            val newCode = codeState.toMutableList()
                            newCode[i] = filtered
                            codeState = newCode
                            onOtpChange(newCode.joinToString(""))
                            if (i < otpLength - 1) {
                                focusRequesters[i + 1].requestFocus()
                            }
                        }
                        // Clearing the current box via soft keyboard backspace
                        filtered.isEmpty() -> {
                            if (codeState[i].isNotEmpty()) {
                                val newCode = codeState.toMutableList()
                                newCode[i] = ""
                                codeState = newCode
                                onOtpChange(newCode.joinToString(""))
                            }
                        }
                    }
                },
                modifier = Modifier
                    .width(48.dp)
                    .height(56.dp)
                    .focusRequester(focusRequesters[i])
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                            if (codeState[i].isEmpty() && i > 0) {
                                val newCode = codeState.toMutableList()
                                newCode[i - 1] = ""
                                codeState = newCode
                                onOtpChange(newCode.joinToString(""))
                                focusRequesters[i - 1].requestFocus()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
