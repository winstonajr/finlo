package com.seunome.finlo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.seunome.finlo.presentation.accounts.AccountListScreen
import com.seunome.finlo.presentation.accounts.AccountViewModel
import com.seunome.finlo.presentation.auth.AuthViewModel
import com.seunome.finlo.presentation.auth.LoginScreen
import com.seunome.finlo.presentation.categories.CategoryListScreen
import com.seunome.finlo.presentation.categories.CategoryViewModel
import com.seunome.finlo.presentation.dashboard.DashboardScreen
import com.seunome.finlo.presentation.dashboard.DashboardViewModel
import com.seunome.finlo.presentation.goals.GoalListScreen
import com.seunome.finlo.presentation.goals.GoalViewModel
import com.seunome.finlo.presentation.lock.PinLockScreen
import com.seunome.finlo.presentation.lock.PinLockViewModel
import com.seunome.finlo.presentation.reports.ReportsScreen
import com.seunome.finlo.presentation.reports.ReportsViewModel
import com.seunome.finlo.presentation.settings.SettingsScreen
import com.seunome.finlo.presentation.settings.SettingsViewModel
import com.seunome.finlo.presentation.transactions.AddEditTransactionScreen
import com.seunome.finlo.presentation.transactions.TransactionListScreen
import com.seunome.finlo.presentation.transactions.TransactionViewModel

@Composable
fun FinloNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            val vm: DashboardViewModel = hiltViewModel()
            DashboardScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.Transactions.route) {
            val vm: TransactionViewModel = hiltViewModel()
            TransactionListScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.AddTransaction.route) {
            val vm: TransactionViewModel = hiltViewModel()
            AddEditTransactionScreen(
                transactionId = null,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStack ->
            val txId = backStack.arguments?.getString("transactionId")
            val vm: TransactionViewModel = hiltViewModel()
            AddEditTransactionScreen(
                transactionId = txId,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Accounts.route) {
            val vm: AccountViewModel = hiltViewModel()
            AccountListScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.Categories.route) {
            val vm: CategoryViewModel = hiltViewModel()
            CategoryListScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.Goals.route) {
            val vm: GoalViewModel = hiltViewModel()
            GoalListScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.Reports.route) {
            val vm: ReportsViewModel = hiltViewModel()
            ReportsScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = vm,
                onNavigate = { navController.navigate(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Login.route) {
            val vm: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = vm,
                onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PinLock.route) {
            val vm: PinLockViewModel = hiltViewModel()
            PinLockScreen(
                viewModel = vm,
                onUnlocked = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.PinLock.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
