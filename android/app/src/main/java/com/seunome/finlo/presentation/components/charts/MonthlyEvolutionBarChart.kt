package com.seunome.finlo.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seunome.finlo.core.designsystem.FinloExpense
import com.seunome.finlo.core.designsystem.FinloIncome

data class MonthEvolutionData(
    val monthLabel: String,
    val incomeCents: Long,
    val expenseCents: Long
)

@Composable
fun MonthlyEvolutionBarChart(
    data: List<MonthEvolutionData>,
    modifier: Modifier = Modifier,
    chartHeightDp: Int = 180
) {
    if (data.isEmpty()) return

    val maxAmount = data.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) }?.coerceAtLeast(100L) ?: 100L

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = FinloIncome) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Receitas", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = FinloExpense) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Despesas", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp.dp)
                .padding(horizontal = 16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 30f // Leave 30f for bottom baseline
            val groupWidth = canvasWidth / data.size
            val barWidth = (groupWidth * 0.35f).coerceAtMost(24f)

            // Baseline
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, canvasHeight),
                strokeWidth = 2f
            )

            data.forEachIndexed { index, item ->
                val centerX = (index * groupWidth) + (groupWidth / 2)

                // Income bar
                val incomeHeight = (item.incomeCents.toFloat() / maxAmount.toFloat()) * (canvasHeight - 20f)
                val incomeTop = canvasHeight - incomeHeight
                val incomeLeft = centerX - barWidth - 4f
                drawRoundRect(
                    color = FinloIncome,
                    topLeft = Offset(incomeLeft, incomeTop),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Expense bar
                val expenseHeight = (item.expenseCents.toFloat() / maxAmount.toFloat()) * (canvasHeight - 20f)
                val expenseTop = canvasHeight - expenseHeight
                val expenseLeft = centerX + 4f
                drawRoundRect(
                    color = FinloExpense,
                    topLeft = Offset(expenseLeft, expenseTop),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Text(
                    text = item.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
