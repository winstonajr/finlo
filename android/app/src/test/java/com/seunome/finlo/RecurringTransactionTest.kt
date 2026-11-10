package com.seunome.finlo

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringTransactionTest {

    @Test
    fun testRN06_installmentSplittingPreservesExactCents() {
        val totalAmountCents = 10000L // R$ 100,00
        val totalInstallments = 3

        val baseCentsPerInstallment = totalAmountCents / totalInstallments // 3333
        val remainder = totalAmountCents % totalInstallments // 1

        val installments = mutableListOf<Long>()
        for (i in 1..totalInstallments) {
            val amount = if (i == 1) {
                baseCentsPerInstallment + remainder
            } else {
                baseCentsPerInstallment
            }
            installments.add(amount)
        }

        assertEquals(listOf(3334L, 3333L, 3333L), installments)
        assertEquals(totalAmountCents, installments.sum())
    }
}
