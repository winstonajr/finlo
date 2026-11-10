package com.seunome.finlo.presentation.reports

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloGreenPrimary
import com.seunome.finlo.core.designsystem.FinloIncome
import com.seunome.finlo.presentation.components.AppBottomBar
import com.seunome.finlo.presentation.components.charts.CategoryDonutChart
import com.seunome.finlo.presentation.components.charts.MonthlyEvolutionBarChart
import com.seunome.finlo.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Relatórios Financeiros") })
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Reports.route,
                onNavigate = onNavigate
            )
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
                            text = "Comparativo Mensal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Mês Atual", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "Rec: R$ %.2f".format(state.currentMonthIncome / 100.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinloIncome
                                )
                                Text(
                                    "Des: R$ %.2f".format(state.currentMonthExpense / 100.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinloExpense
                                )
                            }

                            Column {
                                Text("Mês Anterior", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "Rec: R$ %.2f".format(state.previousMonthIncome / 100.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinloIncome
                                )
                                Text(
                                    "Des: R$ %.2f".format(state.previousMonthExpense / 100.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinloExpense
                                )
                            }
                        }
                    }
                }
            }

            if (state.evolution.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Evolução das Finanças",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MonthlyEvolutionBarChart(data = state.evolution)
                        }
                    }
                }
            }

            if (state.slices.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Composição de Despesas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CategoryDonutChart(slices = state.slices)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Exportar Relatórios Locais",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportCsv(context) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text("CSV")
                            }

                            Button(
                                onClick = {
                                    viewModel.exportPdf(context) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FinloGreenPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text("PDF")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
