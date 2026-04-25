package com.vibra.bus.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vibra.bus.util.LatLng

@Composable
expect fun MapViewComposable(
    modifier: Modifier = Modifier,
    userLocation: LatLng? = null,
)
