package com.seunome.finlo.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seunome.finlo.presentation.navigation.Screen

@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    NavigationBar(
        modifier = Modifier
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = shape
            )
            .height(72.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Início") },
            label = { Text("Início", fontSize = 11.sp, fontWeight = if (currentRoute == Screen.Dashboard.route) FontWeight.Bold else FontWeight.Medium) },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = { onNavigate(Screen.Dashboard.route) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Extrato") },
            label = { Text("Extrato", fontSize = 11.sp, fontWeight = if (currentRoute == Screen.Transactions.route) FontWeight.Bold else FontWeight.Medium) },
            selected = currentRoute == Screen.Transactions.route,
            onClick = { onNavigate(Screen.Transactions.route) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Contas") },
            label = { Text("Contas", fontSize = 11.sp, fontWeight = if (currentRoute == Screen.Accounts.route) FontWeight.Bold else FontWeight.Medium) },
            selected = currentRoute == Screen.Accounts.route,
            onClick = { onNavigate(Screen.Accounts.route) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Flag, contentDescription = "Metas") },
            label = { Text("Metas", fontSize = 11.sp, fontWeight = if (currentRoute == Screen.Goals.route) FontWeight.Bold else FontWeight.Medium) },
            selected = currentRoute == Screen.Goals.route,
            onClick = { onNavigate(Screen.Goals.route) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Relatórios") },
            label = { Text("Relatórios", fontSize = 11.sp, fontWeight = if (currentRoute == Screen.Reports.route) FontWeight.Bold else FontWeight.Medium) },
            selected = currentRoute == Screen.Reports.route,
            onClick = { onNavigate(Screen.Reports.route) },
            colors = itemColors
        )
    }
}
