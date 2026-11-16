package com.seunome.finlo.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloExpenseContainer
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.core.designsystem.FinloIncome
import com.seunome.finlo.core.designsystem.FinloIncomeContainer
import com.seunome.finlo.core.designsystem.FinloTransfer
import com.seunome.finlo.core.designsystem.FinloTransferContainer
import com.seunome.finlo.domain.model.Frequency
import com.seunome.finlo.domain.model.TransactionType
import com.seunome.finlo.presentation.components.CurrencyTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var dateEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var receiptAttachment by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf("") }
    var selectedTargetAccountId by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var isInstallment by remember { mutableStateOf(false) }
    var totalInstallments by remember { mutableIntStateOf(2) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf(Frequency.MONTHLY) }

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
                dateEpochMs = tx.dateEpochMs
                receiptAttachment = tx.receiptAttachment ?: ""
                selectedAccountId = tx.accountId
                selectedTargetAccountId = tx.targetAccountId ?: ""
                selectedCategoryId = tx.categoryId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (transactionId == null) "Nova Transação" else "Editar Transação",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TypeSegmentButton(
                    label = "Despesa",
                    selected = type == TransactionType.EXPENSE,
                    activeColor = FinloExpense,
                    activeContainer = FinloExpenseContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { type = TransactionType.EXPENSE }
                )
                TypeSegmentButton(
                    label = "Receita",
                    selected = type == TransactionType.INCOME,
                    activeColor = FinloIncome,
                    activeContainer = FinloIncomeContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { type = TransactionType.INCOME }
                )
                TypeSegmentButton(
                    label = "Transf.",
                    selected = type == TransactionType.TRANSFER,
                    activeColor = FinloTransfer,
                    activeContainer = FinloTransferContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { type = TransactionType.TRANSFER }
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
                placeholder = { Text("Ex: Supermercado, Salário...") },
                shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    state.accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name, fontWeight = FontWeight.Medium) },
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = targetDropdownExpanded,
                        onDismissRequest = { targetDropdownExpanded = false }
                    ) {
                        state.accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, fontWeight = FontWeight.Medium) },
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Date display (RF08)
            val dateFormatted = remember(dateEpochMs) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(dateEpochMs))
            }
            OutlinedTextField(
                value = dateFormatted,
                onValueChange = {},
                readOnly = true,
                label = { Text("Data da Transação") },
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Attachment input (RF08)
            OutlinedTextField(
                value = receiptAttachment,
                onValueChange = { receiptAttachment = it },
                label = { Text("Anexo de Comprovante") },
                placeholder = { Text("Nome do arquivo ou URL (opcional)") },
                trailingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Installments section (RN06)
            if (type == TransactionType.EXPENSE && transactionId == null && !isRecurring) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isInstallment,
                        onCheckedChange = { isInstallment = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Compra parcelada no cartão", fontWeight = FontWeight.Medium)
                }

                if (isInstallment) {
                    OutlinedTextField(
                        value = totalInstallments.toString(),
                        onValueChange = { totalInstallments = it.toIntOrNull()?.coerceIn(2, 48) ?: 2 },
                        label = { Text("Número de Parcelas (2 a 48)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Recurring transaction section (RF10 & RN05)
            if (transactionId == null && !isInstallment) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Transação Recorrente (mensalidade, salário)", fontWeight = FontWeight.Medium)
                }

                if (isRecurring) {
                    var freqExpanded by remember { mutableStateOf(false) }
                    val freqLabel = when (recurringFrequency) {
                        Frequency.DAILY -> "Diária"
                        Frequency.WEEKLY -> "Semanal"
                        Frequency.MONTHLY -> "Mensal"
                        Frequency.YEARLY -> "Anual"
                    }

                    ExposedDropdownMenuBox(
                        expanded = freqExpanded,
                        onExpandedChange = { freqExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = freqLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequência de Repetição") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = freqExpanded,
                            onDismissRequest = { freqExpanded = false }
                        ) {
                            Frequency.entries.forEach { freq ->
                                val text = when (freq) {
                                    Frequency.DAILY -> "Diária"
                                    Frequency.WEEKLY -> "Semanal"
                                    Frequency.MONTHLY -> "Mensal"
                                    Frequency.YEARLY -> "Anual"
                                }
                                DropdownMenuItem(
                                    text = { Text(text) },
                                    onClick = {
                                        recurringFrequency = freq
                                        freqExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            dateEpochMs = dateEpochMs,
                            receiptAttachment = receiptAttachment.ifBlank { null },
                            isInstallment = isInstallment,
                            totalInstallments = totalInstallments,
                            isRecurring = isRecurring,
                            recurringFrequency = recurringFrequency
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = amountCents > 0L && selectedAccountId.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (type) {
                        TransactionType.EXPENSE -> FinloExpense
                        TransactionType.INCOME -> FinloGreenPrimary
                        TransactionType.TRANSFER -> FinloTransfer
                    }
                )
            ) {
                Text(
                    "Salvar Transação",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TypeSegmentButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeContainer: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
