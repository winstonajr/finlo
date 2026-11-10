package com.seunome.finlo.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.seunome.finlo.presentation.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Extrato de Transações") })
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Transactions.route,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.AddTransaction.route) },
                containerColor = FinloGreenPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Transação")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Buscar por descrição...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedType == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text("Todos") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedType == TransactionType.INCOME,
                        onClick = { viewModel.setTypeFilter(TransactionType.INCOME) },
                        label = { Text("Receitas") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedType == TransactionType.EXPENSE,
                        onClick = { viewModel.setTypeFilter(TransactionType.EXPENSE) },
                        label = { Text("Despesas") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedType == TransactionType.TRANSFER,
                        onClick = { viewModel.setTypeFilter(TransactionType.TRANSFER) },
                        label = { Text("Transferências") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.transactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nenhuma transação encontrada",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            onClick = { onNavigate(Screen.EditTransaction.createRoute(tx.id)) },
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(transaction.dateEpochMs))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (transaction.description.isBlank()) "Sem descrição" else transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (transaction.installmentNumber != null && transaction.totalInstallments != null) {
                    Text(
                        text = "Parcela ${transaction.installmentNumber}/${transaction.totalInstallments}",
                        style = MaterialTheme.typography.labelSmall,
                        color = FinloGreenPrimary
                    )
                }
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prefix + "R$ " + "%.2f".format(transaction.amountCents / 100.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
