package com.seunome.finlo

import com.seunome.finlo.data.repository.RecurringRuleRepositoryImpl
import com.seunome.finlo.domain.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

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

    @Test
    fun testRN05_recurringNextDueDateCalculation() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 15, 10, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val initialEpoch = cal.timeInMillis

        // Monthly progression
        val nextMonthly = RecurringRuleRepositoryImpl.calculateNextDueDate(initialEpoch, Frequency.MONTHLY, 15)
        val expectedMonthlyCal = Calendar.getInstance().apply {
            timeInMillis = initialEpoch
            add(Calendar.MONTH, 1)
        }
        assertEquals(expectedMonthlyCal.timeInMillis, nextMonthly)

        // Weekly progression
        val nextWeekly = RecurringRuleRepositoryImpl.calculateNextDueDate(initialEpoch, Frequency.WEEKLY, 15)
        val expectedWeeklyCal = Calendar.getInstance().apply {
            timeInMillis = initialEpoch
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        assertEquals(expectedWeeklyCal.timeInMillis, nextWeekly)

        // Daily progression
        val nextDaily = RecurringRuleRepositoryImpl.calculateNextDueDate(initialEpoch, Frequency.DAILY, 15)
        val expectedDailyCal = Calendar.getInstance().apply {
            timeInMillis = initialEpoch
            add(Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals(expectedDailyCal.timeInMillis, nextDaily)

        // Yearly progression
        val nextYearly = RecurringRuleRepositoryImpl.calculateNextDueDate(initialEpoch, Frequency.YEARLY, 15)
        val expectedYearlyCal = Calendar.getInstance().apply {
            timeInMillis = initialEpoch
            add(Calendar.YEAR, 1)
        }
        assertEquals(expectedYearlyCal.timeInMillis, nextYearly)
    }

    @Test
    fun testRN05_dayOfMonthClampingForShorterMonths() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 31, 12, 0, 0)
        val jan31 = cal.timeInMillis

        // Advancing from Jan 31 to Feb should clamp to Feb 28 (or 29)
        val nextDueFeb = RecurringRuleRepositoryImpl.calculateNextDueDate(jan31, Frequency.MONTHLY, 31)
        val febCal = Calendar.getInstance().apply { timeInMillis = nextDueFeb }
        assertEquals(Calendar.FEBRUARY, febCal.get(Calendar.MONTH))
        assertTrue(febCal.get(Calendar.DAY_OF_MONTH) in 28..29)
    }
}
