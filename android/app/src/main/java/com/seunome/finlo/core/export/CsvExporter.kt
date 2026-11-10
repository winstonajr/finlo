package com.seunome.finlo.core.export

import com.seunome.finlo.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun exportToCsv(
        transactions: List<Transaction>,
        accountNames: Map<String, String>,
        categoryNames: Map<String, String>
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()

        sb.append("ID,Data,Tipo,Conta,Categoria,Descrição,Valor,Parcela\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.dateEpochMs))
            val accountName = accountNames[tx.accountId] ?: tx.accountId
            val categoryName = categoryNames[tx.categoryId] ?: tx.categoryId
            val amountFormatted = "%.2f".format(Locale.US, tx.amountCents / 100.0)
            val installmentStr = if (tx.installmentNumber != null && tx.totalInstallments != null) {
                "${tx.installmentNumber}/${tx.totalInstallments}"
            } else ""

            val escapedDesc = tx.description.replace("\"", "\"\"")

            sb.append("${tx.id},")
            sb.append("\"$dateStr\",")
            sb.append("${tx.type.name},")
            sb.append("\"$accountName\",")
            sb.append("\"$categoryName\",")
            sb.append("\"$escapedDesc\",")
            sb.append("$amountFormatted,")
            sb.append("\"$installmentStr\"\n")
        }

        return sb.toString()
    }
}
