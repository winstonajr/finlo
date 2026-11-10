package com.seunome.finlo.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.core.sync.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    syncState: SyncState = SyncState.Idle,
    onSyncClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSyncClick) {
                    when (syncState) {
                        is SyncState.Syncing -> {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Sincronizando...",
                                tint = FinloGreenPrimary
                            )
                        }
                        is SyncState.Success -> {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Sincronizado",
                                tint = FinloGreenPrimary
                            )
                        }
                        is SyncState.Error -> {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = "Erro na sincronização",
                                tint = FinloExpense
                            )
                        }
                        is SyncState.Idle -> {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = "Sincronização ociosa",
                                tint = Color.Gray
                            )
                        }
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações")
                }
            }
        }
    )
}
