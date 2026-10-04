package com.vibra.bus.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.vibra.bus.presentation.theme.AppShape

/** Campo de busqueda con filtro instantaneo: el llamador filtra su lista con el texto actual. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        shape = AppShape.InputField,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

/** Coincidencia sin distinguir mayusculas ni acentos comunes. */
fun matchesQuery(query: String, vararg fields: String): Boolean {
    if (query.isBlank()) return true
    val q = normalizeForSearch(query)
    return fields.any { normalizeForSearch(it).contains(q) }
}

private fun normalizeForSearch(s: String): String = buildString(s.length) {
    for (c in s.trim().lowercase()) {
        append(
            when (c) {
                'á', 'à', 'ä' -> 'a'
                'é', 'è', 'ë' -> 'e'
                'í', 'ì', 'ï' -> 'i'
                'ó', 'ò', 'ö' -> 'o'
                'ú', 'ù', 'ü' -> 'u'
                'ñ' -> 'n'
                else -> c
            }
        )
    }
}
