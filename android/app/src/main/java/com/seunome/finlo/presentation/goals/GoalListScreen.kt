package com.seunome.finlo.presentation.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.domain.model.Goal
import com.seunome.finlo.presentation.components.AppBottomBar
import com.seunome.finlo.presentation.components.CurrencyTextField
import com.seunome.finlo.presentation.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    viewModel: GoalViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var goalForDeposit by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Metas de Economia") })
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Goals.route,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FinloGreenPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Meta")
            }
        }
    ) { padding ->
        if (state.goals.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nenhuma meta cadastrada ainda.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    val progress = if (goal.targetAmountCents > 0) {
                        (goal.currentAmountCents.toFloat() / goal.targetAmountCents.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val percentage = (progress * 100).toInt()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val deadlineStr = dateFormat.format(Date(goal.targetDateEpochMs))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = goal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Prazo: $deadlineStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Row {
                                    IconButton(onClick = { goalForDeposit = goal }) {
                                        Icon(Icons.Default.Payments, contentDescription = "Adicionar valor")
                                    }
                                    IconButton(onClick = { viewModel.deleteGoal(goal.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = FinloGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "R$ %.2f de R$ %.2f".format(goal.currentAmountCents / 100.0, goal.targetAmountCents / 100.0),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var targetCents by remember { mutableLongStateOf(0L) }
        var currentCents by remember { mutableLongStateOf(0L) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nova Meta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da Meta (ex: Viagem)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    CurrencyTextField(
                        amountCents = targetCents,
                        onAmountChange = { targetCents = it },
                        label = "Valor Alvo",
                        modifier = Modifier.fillMaxWidth()
                    )
                    CurrencyTextField(
                        amountCents = currentCents,
                        onAmountChange = { currentCents = it },
                        label = "Valor Já Guardado",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && targetCents > 0L) {
                            viewModel.saveGoal(
                                id = null,
                                name = name,
                                targetAmountCents = targetCents,
                                currentAmountCents = currentCents,
                                targetDateEpochMs = System.currentTimeMillis() + 180L * 24 * 3600 * 1000
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Criar Meta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (goalForDeposit != null) {
        var depositCents by remember { mutableLongStateOf(0L) }

        AlertDialog(
            onDismissRequest = { goalForDeposit = null },
            title = { Text("Aportar na Meta: ${goalForDeposit?.name}") },
            text = {
                CurrencyTextField(
                    amountCents = depositCents,
                    onAmountChange = { depositCents = it },
                    label = "Valor do Aporte",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (depositCents > 0L && goalForDeposit != null) {
                            viewModel.addProgress(
                                goalId = goalForDeposit!!.id,
                                currentAmountCents = goalForDeposit!!.currentAmountCents,
                                addedCents = depositCents
                            )
                            goalForDeposit = null
                        }
                    }
                ) {
                    Text("Confirmar Aporte")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalForDeposit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
