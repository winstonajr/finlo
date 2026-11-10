package com.seunome.finlo.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DonutSlice(
    val categoryName: String,
    val amountCents: Long,
    val color: Color
)

@Composable
fun CategoryDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    chartSize: Int = 220,
    strokeWidthDp: Float = 32f
) {
    val totalCents = slices.sumOf { it.amountCents }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(chartSize.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(chartSize.dp)) {
                val diameter = size.minDimension - strokeWidthDp
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val arcSize = Size(diameter, diameter)

                if (totalCents == 0L) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthDp)
                    )
                } else {
                    var currentStartAngle = -90f
                    for (slice in slices) {
                        val sweepAngle = (slice.amountCents.toFloat() / totalCents.toFloat()) * 360f
                        drawArc(
                            color = slice.color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle - 1f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthDp, cap = StrokeCap.Round)
                        )
                        currentStartAngle += sweepAngle
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Gasto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "R$ " + "%.2f".format(totalCents / 100.0),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            for (slice in slices.take(5)) {
                val percentage = if (totalCents > 0) (slice.amountCents * 100.0 / totalCents) else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = CircleShape,
                            color = slice.color
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = slice.categoryName, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "R$ %.2f (%.1f%%)".format(slice.amountCents / 100.0, percentage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
