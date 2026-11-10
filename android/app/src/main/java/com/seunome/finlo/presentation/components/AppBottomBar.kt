package com.seunome.finlo.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.seunome.finlo.presentation.navigation.Screen

@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Início") },
            label = { Text("Início") },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = { onNavigate(Screen.Dashboard.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Receipt, contentDescription = "Transações") },
            label = { Text("Extrato") },
            selected = currentRoute == Screen.Transactions.route,
            onClick = { onNavigate(Screen.Transactions.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Contas") },
            label = { Text("Contas") },
            selected = currentRoute == Screen.Accounts.route,
            onClick = { onNavigate(Screen.Accounts.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Category, contentDescription = "Categorias") },
            label = { Text("Categorias") },
            selected = currentRoute == Screen.Categories.route,
            onClick = { onNavigate(Screen.Categories.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Flag, contentDescription = "Metas") },
            label = { Text("Metas") },
            selected = currentRoute == Screen.Goals.route,
            onClick = { onNavigate(Screen.Goals.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Relatórios") },
            label = { Text("Relatórios") },
            selected = currentRoute == Screen.Reports.route,
            onClick = { onNavigate(Screen.Reports.route) }
        )
    }
}
