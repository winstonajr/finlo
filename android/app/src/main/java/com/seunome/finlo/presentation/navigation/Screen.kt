package com.seunome.finlo.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object AddTransaction : Screen("transaction/add")
    object EditTransaction : Screen("transaction/edit/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction/edit/$transactionId"
    }
    object Accounts : Screen("accounts")
    object Categories : Screen("categories")
    object Goals : Screen("goals")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object Login : Screen("login")
    object PinLock : Screen("pin_lock")
}
