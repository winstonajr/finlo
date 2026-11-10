package com.seunome.finlo.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale

@Composable
fun CurrencyTextField(
    amountCents: Long,
    onAmountChange: (Long) -> Unit,
    label: String = "Valor",
    modifier: Modifier = Modifier
) {
    val formatted = "R$ " + "%.2f".format(Locale.getDefault(), amountCents / 100.0)

    OutlinedTextField(
        value = formatted,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }
            val newCents = digitsOnly.toLongOrNull() ?: 0L
            onAmountChange(newCents)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
