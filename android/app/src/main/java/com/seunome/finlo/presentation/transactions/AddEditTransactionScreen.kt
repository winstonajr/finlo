package com.seunome.finlo.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seunome.finlo.domain.model.TransactionType
import com.seunome.finlo.presentation.components.CurrencyTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: String? = null,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountCents by remember { mutableLongStateOf(0L) }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf("") }
    var selectedTargetAccountId by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var isInstallment by remember { mutableStateOf(false) }
    var totalInstallments by remember { mutableIntStateOf(2) }

    LaunchedEffect(state.accounts, state.categories) {
        if (selectedAccountId.isEmpty() && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().id
        }
        if (selectedCategoryId.isEmpty() && state.categories.isNotEmpty()) {
            selectedCategoryId = state.categories.first().id
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            val tx = viewModel.getTransactionById(transactionId)
            if (tx != null) {
                type = tx.type
                amountCents = tx.amountCents
                description = tx.description
                selectedAccountId = tx.accountId
                selectedTargetAccountId = tx.targetAccountId ?: ""
                selectedCategoryId = tx.categoryId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Nova Transação" else "Editar Transação") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(selectedTabIndex = type.ordinal) {
                Tab(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    text = { Text("Despesa") }
                )
                Tab(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    text = { Text("Receita") }
                )
                Tab(
                    selected = type == TransactionType.TRANSFER,
                    onClick = { type = TransactionType.TRANSFER },
                    text = { Text("Transferência") }
                )
            }

            CurrencyTextField(
                amountCents = amountCents,
                onAmountChange = { amountCents = it },
                label = "Valor da Transação",
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição / Estabelecimento") },
                modifier = Modifier.fillMaxWidth()
            )

            // Account selection
            var accountDropdownExpanded by remember { mutableStateOf(false) }
            val currentAccount = state.accounts.find { it.id == selectedAccountId }

            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentAccount?.name ?: "Selecione a conta",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (type == TransactionType.TRANSFER) "Conta de Origem" else "Conta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    state.accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = {
                                selectedAccountId = acc.id
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Target account if transfer
            if (type == TransactionType.TRANSFER) {
                var targetDropdownExpanded by remember { mutableStateOf(false) }
                val targetAccount = state.accounts.find { it.id == selectedTargetAccountId }

                ExposedDropdownMenuBox(
                    expanded = targetDropdownExpanded,
                    onExpandedChange = { targetDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = targetAccount?.name ?: "Selecione a conta destino",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Conta de Destino") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = targetDropdownExpanded,
                        onDismissRequest = { targetDropdownExpanded = false }
                    ) {
                        state.accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    selectedTargetAccountId = acc.id
                                    targetDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Category selection
            var categoryDropdownExpanded by remember { mutableStateOf(false) }
            val currentCategory = state.categories.find { it.id == selectedCategoryId }

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentCategory?.name ?: "Selecione a categoria",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Installments section
            if (type == TransactionType.EXPENSE && transactionId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isInstallment,
                        onCheckedChange = { isInstallment = it }
                    )
                    Text("Compra parcelada no cartão")
                }

                if (isInstallment) {
                    OutlinedTextField(
                        value = totalInstallments.toString(),
                        onValueChange = { totalInstallments = it.toIntOrNull()?.coerceIn(2, 48) ?: 2 },
                        label = { Text("Número de Parcelas (2 a 48)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (amountCents > 0L && selectedAccountId.isNotBlank()) {
                        viewModel.saveTransaction(
                            id = transactionId,
                            type = type,
                            amountCents = amountCents,
                            accountId = selectedAccountId,
                            targetAccountId = if (type == TransactionType.TRANSFER) selectedTargetAccountId else null,
                            categoryId = selectedCategoryId,
                            description = description,
                            dateEpochMs = System.currentTimeMillis(),
                            isInstallment = isInstallment,
                            totalInstallments = totalInstallments
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amountCents > 0L && selectedAccountId.isNotBlank()
            ) {
                Text("Salvar Transação")
            }
        }
    }
}
