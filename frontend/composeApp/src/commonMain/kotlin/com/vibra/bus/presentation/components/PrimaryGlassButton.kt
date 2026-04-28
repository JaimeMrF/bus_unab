package com.vibra.bus.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Primary Glass Button with Material 3 styling
 * Wrapper around GlassButton with primary styling
 */
@Composable
fun PrimaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    GlassButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        loading = loading,
        enabled = enabled
    )
}
