package com.seunome.finlo.presentation.categories

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.domain.model.Category
import com.seunome.finlo.presentation.components.AppBottomBar
import com.seunome.finlo.presentation.components.CurrencyTextField
import com.seunome.finlo.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Categorias e Orçamentos") })
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Categories.route,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedCategory = null
                    showEditDialog = true
                },
                containerColor = FinloGreenPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Categoria")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.categories, key = { it.category.id }) { item ->
                val cat = item.category
                val progress = if (item.budgetCents > 0) {
                    (item.spentCents.toFloat() / item.budgetCents.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(onClick = {
                                    selectedCategory = cat
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { categoryToDelete = cat }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (item.budgetCents > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (item.isOver100Percent) FinloExpense else FinloGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Gasto: R$ %.2f".format(item.spentCents / 100.0),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Teto: R$ %.2f".format(item.budgetCents / 100.0),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        if (item.isOver100Percent) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = FinloExpense)
                                Text(
                                    text = " Atenção: 100% do orçamento estourado!",
                                    color = FinloExpense,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (item.isOver80Percent) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                                Text(
                                    text = " Atenção: 80% do limite atingido!",
                                    color = Color(0xFFFF9800),
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

    if (showEditDialog) {
        var name by remember { mutableStateOf(selectedCategory?.name ?: "") }
        var budgetCents by remember { mutableLongStateOf(selectedCategory?.budgetCents ?: 0L) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (selectedCategory == null) "Nova Categoria" else "Editar Categoria") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da Categoria") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    CurrencyTextField(
                        amountCents = budgetCents,
                        onAmountChange = { budgetCents = it },
                        label = "Orçamento Mensal (opcional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveCategory(
                                id = selectedCategory?.id,
                                name = name,
                                colorHex = "#4CAF50",
                                icon = "category",
                                budgetCents = budgetCents
                            )
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Reassignment dialog on delete (enforces RN04)
    if (categoryToDelete != null) {
        val otherCategories = state.categories.filter { it.category.id != categoryToDelete?.id }
        var fallbackId by remember { mutableStateOf(otherCategories.firstOrNull()?.category?.id ?: "") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Reatribuir Transações") },
            text = {
                Column {
                    Text("A regra de negócio exige que as transações desta categoria sejam reatribuídas a outra antes de excluir:")
                    Spacer(modifier = Modifier.height(12.dp))

                    val selectedFallback = otherCategories.find { it.category.id == fallbackId }
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFallback?.category?.name ?: "Selecione",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nova Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            otherCategories.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.category.name) },
                                    onClick = {
                                        fallbackId = item.category.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fallbackId.isNotBlank() && categoryToDelete != null) {
                            viewModel.deleteCategory(categoryToDelete!!.id, fallbackId)
                            categoryToDelete = null
                        }
                    },
                    enabled = fallbackId.isNotBlank()
                ) {
                    Text("Confirmar Exclusão")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
