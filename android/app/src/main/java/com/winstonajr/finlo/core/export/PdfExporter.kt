package com.seunome.finlo.core.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    fun exportToPdf(
        outputStream: OutputStream,
        transactions: List<Transaction>,
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        periodName: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }

        val greenPaint = Paint().apply {
            color = Color.rgb(46, 125, 50)
            textSize = 12f
            isFakeBoldText = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(198, 40, 40)
            textSize = 12f
            isFakeBoldText = true
        }

        canvas.drawText("Relatório Financeiro Finlo", 40f, 50f, titlePaint)
        canvas.drawText("Período: $periodName", 40f, 75f, subtitlePaint)

        val balanceCents = totalIncomeCents - totalExpenseCents
        val incomeStr = "Receitas: R$ " + "%.2f".format(totalIncomeCents / 100.0)
        val expenseStr = "Despesas: R$ " + "%.2f".format(totalExpenseCents / 100.0)
        val balanceStr = "Saldo do Período: R$ " + "%.2f".format(balanceCents / 100.0)

        canvas.drawText(incomeStr, 40f, 105f, greenPaint)
        canvas.drawText(expenseStr, 220f, 105f, redPaint)
        canvas.drawText(balanceStr, 400f, 105f, subtitlePaint)

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 120f, 555f, 120f, linePaint)

        var y = 140f
        canvas.drawText("Data", 40f, y, headerPaint)
        canvas.drawText("Tipo", 120f, y, headerPaint)
        canvas.drawText("Descrição", 200f, y, headerPaint)
        canvas.drawText("Valor", 480f, y, headerPaint)

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (tx in transactions) {
            if (y > 800f) break

            val dateStr = dateFormat.format(Date(tx.dateEpochMs))
            val amountFormatted = "R$ " + "%.2f".format(tx.amountCents / 100.0)
            val desc = if (tx.description.length > 35) tx.description.take(32) + "..." else tx.description

            canvas.drawText(dateStr, 40f, y, textPaint)
            canvas.drawText(tx.type.name, 120f, y, textPaint)
            canvas.drawText(desc, 200f, y, textPaint)

            val amountPaint = when (tx.type) {
                TransactionType.INCOME -> greenPaint
                TransactionType.EXPENSE -> redPaint
                TransactionType.TRANSFER -> subtitlePaint
            }
            canvas.drawText(amountFormatted, 480f, y, amountPaint)

            y += 20f
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
