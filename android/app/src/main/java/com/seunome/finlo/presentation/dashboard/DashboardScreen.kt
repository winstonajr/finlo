package com.seunome.finlo.presentation.dashboard

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.core.designsystem.FinloIncome
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType
import com.seunome.finlo.presentation.components.AppBottomBar
import com.seunome.finlo.presentation.components.AppTopBar
import com.seunome.finlo.presentation.components.charts.CategoryDonutChart
import com.seunome.finlo.presentation.components.charts.MonthlyEvolutionBarChart
import com.seunome.finlo.presentation.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Finlo",
                syncState = state.syncState,
                onSyncClick = { viewModel.triggerSync() },
                onSettingsClick = { onNavigate(Screen.Settings.route) }
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Dashboard.route,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.AddTransaction.route) },
                containerColor = FinloGreenPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Transação")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Saldo Total Consolidado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "R$ " + "%.2f".format(state.totalBalanceCents / 100.0),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = FinloIncome)
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text("Receitas", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "R$ %.2f".format(state.monthlyIncomeCents / 100.0),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FinloIncome
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = FinloExpense)
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text("Despesas", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "R$ %.2f".format(state.monthlyExpenseCents / 100.0),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FinloExpense
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.categorySlices.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Despesas por Categoria",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CategoryDonutChart(slices = state.categorySlices)
                        }
                    }
                }
            }

            if (state.evolutionData.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Evolução Financeira",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MonthlyEvolutionBarChart(data = state.evolutionData)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Transações Recentes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Text(
                        text = "Nenhuma transação registrada ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(state.recentTransactions) { tx ->
                    TransactionItemView(
                        transaction = tx,
                        onClick = { onNavigate(Screen.EditTransaction.createRoute(tx.id)) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun TransactionItemView(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(transaction.dateEpochMs))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (transaction.description.isBlank()) "Sem descrição" else transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            val prefix = when (transaction.type) {
                TransactionType.INCOME -> "+ "
                TransactionType.EXPENSE -> "- "
                TransactionType.TRANSFER -> ""
            }
            val amountColor = when (transaction.type) {
                TransactionType.INCOME -> FinloIncome
                TransactionType.EXPENSE -> FinloExpense
                TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = prefix + "R$ " + "%.2f".format(transaction.amountCents / 100.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
