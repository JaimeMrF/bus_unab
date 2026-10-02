package com.vibra.bus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.theme.LocalBrand
import com.vibra.bus.presentation.theme.LocalIsDarkTheme

/** Placeholder neutro: círculo tonal con un icono. Sin marca ni mascota propia. */
@Composable
fun NeutralBadge(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.DirectionsBus,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxSize(0.5f),
        )
    }
}

/** Mascota de la organización (URL). Sin URL o si falla la carga, muestra el placeholder neutro. */
@Composable
fun BrandMascot(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.DirectionsBus,
) {
    val url = LocalBrand.current.mascotUrl
    if (url.isNullOrBlank()) {
        NeutralBadge(modifier, icon)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            loading = { NeutralBadge(Modifier.fillMaxSize(), icon) },
            error = { NeutralBadge(Modifier.fillMaxSize(), icon) },
        )
    }
}

/**
 * Logo de la organización (variante oscura si aplica). Fallback: inicial del nombre sobre
 * color primario, nunca una marca ajena.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val brand = LocalBrand.current
    val url = brand.logoFor(LocalIsDarkTheme.current)
    val description = brand.appName
    if (url.isNullOrBlank()) {
        LogoFallback(brand.appName, modifier)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = description,
            modifier = modifier,
            contentScale = contentScale,
            loading = { LogoFallback(brand.appName, Modifier.fillMaxSize()) },
            error = { LogoFallback(brand.appName, Modifier.fillMaxSize()) },
        )
    }
}

@Composable
private fun LogoFallback(name: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(AppShape.CardLarge)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "•" },
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp),
        )
    }
}
