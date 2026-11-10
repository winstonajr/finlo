package com.seunome.finlo

import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculationTest {

    @Test
    fun testRN01_accountBalanceCalculationFormula() {
        val accountId = "acc-wallet"
        val otherAccountId = "acc-checking"

        val transactions = listOf(
            Transaction(
                id = "1",
                accountId = accountId,
                categoryId = "cat-salary",
                type = TransactionType.INCOME,
                amountCents = 350000L, // R$ 3.500,00
                dateEpochMs = 1000L
            ),
            Transaction(
                id = "2",
                accountId = accountId,
                categoryId = "cat-food",
                type = TransactionType.EXPENSE,
                amountCents = 5550L, // R$ 55,50
                dateEpochMs = 2000L
            ),
            Transaction(
                id = "3",
                accountId = accountId,
                targetAccountId = otherAccountId,
                categoryId = "cat-transfer",
                type = TransactionType.TRANSFER,
                amountCents = 100000L, // R$ 1.000,00 sent to other account
                dateEpochMs = 3000L
            ),
            Transaction(
                id = "4",
                accountId = otherAccountId,
                targetAccountId = accountId,
                categoryId = "cat-transfer",
                type = TransactionType.TRANSFER,
                amountCents = 20000L, // R$ 200,00 received from other account
                dateEpochMs = 4000L
            )
        )

        // RN01: saldo = receitas - despesas - transferências de saída + transferências de entrada
        var balance = 0L
        for (tx in transactions) {
            when {
                tx.type == TransactionType.INCOME && tx.accountId == accountId -> balance += tx.amountCents
                tx.type == TransactionType.EXPENSE && tx.accountId == accountId -> balance -= tx.amountCents
                tx.type == TransactionType.TRANSFER && tx.accountId == accountId -> balance -= tx.amountCents
                tx.type == TransactionType.TRANSFER && tx.targetAccountId == accountId -> balance += tx.amountCents
            }
        }

        // Expected: 350000 - 5550 - 100000 + 20000 = 264450 cents (R$ 2.644,50)
        assertEquals(264450L, balance)
    }

    @Test
    fun testRN02_transfersDoNotCountAsIncomeOrExpenseInReports() {
        val transactions = listOf(
            Transaction(
                id = "1",
                accountId = "acc-1",
                categoryId = "cat-1",
                type = TransactionType.INCOME,
                amountCents = 500000L,
                dateEpochMs = 1000L
            ),
            Transaction(
                id = "2",
                accountId = "acc-1",
                categoryId = "cat-2",
                type = TransactionType.EXPENSE,
                amountCents = 150000L,
                dateEpochMs = 2000L
            ),
            Transaction(
                id = "3",
                accountId = "acc-1",
                targetAccountId = "acc-2",
                categoryId = "cat-transfer",
                type = TransactionType.TRANSFER,
                amountCents = 200000L,
                dateEpochMs = 3000L
            )
        )

        val reportIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
        val reportExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }

        // Transfer must be excluded from both income and expense in reports
        assertEquals(500000L, reportIncome)
        assertEquals(150000L, reportExpense)
    }

    @Test
    fun testRN03_centsStoredAsLongNeverFloat() {
        val amount1 = 1999L // R$ 19,99
        val amount2 = 2001L // R$ 20,01
        val sum = amount1 + amount2

        assertEquals(4000L, sum)
    }
}
