package com.vibra.bus.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    message: String,
    subtitle: String = "",
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text  = "🚌",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text       = message,
            style      = MaterialTheme.typography.titleMedium,
            color      = MaterialTheme.colorScheme.onSurface,
            textAlign  = TextAlign.Center,
        )
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text      = subtitle,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (ctaLabel != null && onCtaClick != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCtaClick,
                colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(ctaLabel, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
