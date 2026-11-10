package com.seunome.finlo.presentation.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.core.security.BiometricHelper

@Composable
fun PinLockScreen(
    viewModel: PinLockViewModel,
    onUnlocked: () -> Unit
) {
    val state by viewModel.lockState.collectAsState()
    var enteredPin by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is LockState.Unlocked) {
            onUnlocked()
        }
    }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            viewModel.verifyPin(enteredPin)
            enteredPin = ""
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Finlo Protegido",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Digite seu PIN de 4 dígitos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 Pin dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val filled = index < enteredPin.length
                    val dotColor = if (state is LockState.Error) FinloExpense else if (filled) FinloGreenPrimary else Color.LightGray
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            if (state is LockState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PIN incorreto. Tente novamente.",
                    color = FinloExpense,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Keypad 1-9
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            for (row in keypad) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        when (key) {
                            "DEL" -> {
                                IconButton(
                                    onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Apagar")
                                }
                            }
                            "BIO" -> {
                                IconButton(
                                    onClick = {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            val bioHelper = BiometricHelper(activity)
                                            if (bioHelper.canAuthenticate()) {
                                                bioHelper.showBiometricPrompt(
                                                    activity = activity,
                                                    onSuccess = { viewModel.unlockByBiometrics() },
                                                    onError = {}
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometria", tint = FinloGreenPrimary)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { if (enteredPin.length < 4) enteredPin += key },
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(text = key, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
