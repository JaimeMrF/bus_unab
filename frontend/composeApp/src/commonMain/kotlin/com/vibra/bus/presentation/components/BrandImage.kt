package com.vibra.bus.presentation.components

import com.vibra.bus.presentation.motion.LocalMotion
import com.vibra.bus.domain.brand.MascotPose
import coil3.request.crossfade
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.compose.LocalPlatformContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
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

/**
 * Mascota de la organización en la pose pedida. Carga con Coil a tamaño fijo (sin decodificar de
 * más), con fundido, cache de memoria y disco y un shimmer como placeholder.
 *
 * Fallback sin hueco: si la pose y la mascota única son null, o la carga falla, con
 * [neutralFallback] se muestra un icono neutro; sin él no se emite nada (el modifier, y por tanto
 * el tamaño reservado, tampoco se aplica).
 *
 * Con movimiento ambiental activo flota 2.5dp y "respira" con un parpadeo de escala; el valor
 * animado se lee en graphicsLayer, sin recomponer.
 */
@Composable
fun BrandMascot(
    modifier: Modifier = Modifier,
    pose: MascotPose = MascotPose.Greeting,
    size: Dp = 120.dp,
    neutralFallback: Boolean = false,
    icon: ImageVector = Icons.Outlined.DirectionsBus,
    animated: Boolean = true,
) {
    val url = LocalBrand.current.poseUrl(pose)
    if (url == null) {
        if (neutralFallback) NeutralBadge(modifier.size(size), icon)
        return
    }
    val env = LocalMotion.current
    val density = LocalDensity.current
    val context = LocalPlatformContext.current
    val px = with(density) { size.roundToPx() }
    val request = remember(url, px) {
        ImageRequest.Builder(context)
            .data(url)
            .size(px, px)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    val float = if (animated && env.ambient) {
        rememberInfiniteTransition(label = "mascot").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "mascot_t",
        )
    } else null
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val t = float?.value ?: 0f
                translationY = -2.5.dp.toPx() * t
                val s = 1f + 0.018f * t
                scaleX = s
                scaleY = s
            },
        contentScale = ContentScale.Fit,
        loading = { ShimmerBox(Modifier.fillMaxSize(), height = size) },
        error = { if (neutralFallback) NeutralBadge(Modifier.fillMaxSize(), icon) },
    )
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
