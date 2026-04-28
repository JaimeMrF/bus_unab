package com.vibra.bus.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Secondary Glass Button with Material 3 styling
 * Wrapper around GlassButton with secondary styling
 */
@Composable
fun SecondaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    showRecommendedBadge: Boolean = false
) {
    GlassButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        loading = loading,
        enabled = enabled
    )
}
