package com.vibra.bus.presentation.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de radios del tenant: "sm" | "md" | "lg" (corner_radius de BrandConfig). */
fun radiusScale(cornerRadius: String): Float = when (cornerRadius) {
    "sm" -> 0.6f
    "lg" -> 1.4f
    else -> 1f
}

/** Shapes de Material 3 escalados por el radio de la marca. */
fun materialShapes(scale: Float) = Shapes(
    extraSmall = RoundedCornerShape((4 * scale).dp),
    small = RoundedCornerShape((8 * scale).dp),
    medium = RoundedCornerShape((12 * scale).dp),
    large = RoundedCornerShape((16 * scale).dp),
    extraLarge = RoundedCornerShape((28 * scale).dp),
)

/** Formas semánticas de la app; todas escalan con la marca. */
class AppShapeSet(private val scale: Float) {
    private fun r(dp: Int): Dp = (dp * scale).dp
    private fun round(dp: Int) = RoundedCornerShape(r(dp))

    val ButtonPrimary: CornerBasedShape = round(14)
    val ButtonSecondary: CornerBasedShape = round(12)
    val InputField: CornerBasedShape = round(14)
    val FloatingActionButton: CornerBasedShape = round(18)
    val Chip: CornerBasedShape = round(10)

    val Card: CornerBasedShape = round(18)
    val CardSmall: CornerBasedShape = round(14)
    val CardLarge: CornerBasedShape = round(24)
    val Dialog: CornerBasedShape = round(28)
    val BottomSheet: CornerBasedShape = RoundedCornerShape(topStart = r(28), topEnd = r(28))
    val Modal: CornerBasedShape = round(24)
    val StopMarker: CornerBasedShape = round(8)
    val ListItem: CornerBasedShape = round(14)
    val QRContainer: CornerBasedShape = round(22)

    // Siempre circulares/píldora: no dependen de la marca.
    val BusMarker = RoundedCornerShape(50)
    val RouteIndicator = RoundedCornerShape(4.dp)
    val StatusBadge = RoundedCornerShape(50)
    val MapButton = RoundedCornerShape(50)
    val Avatar = RoundedCornerShape(50)
}

val LocalAppShapes = staticCompositionLocalOf { AppShapeSet(1f) }

/** Acceso estilo `AppShape.Card` dentro de composables. */
object AppShape {
    val ButtonPrimary @Composable @ReadOnlyComposable get() = LocalAppShapes.current.ButtonPrimary
    val ButtonSecondary @Composable @ReadOnlyComposable get() = LocalAppShapes.current.ButtonSecondary
    val InputField @Composable @ReadOnlyComposable get() = LocalAppShapes.current.InputField
    val FloatingActionButton @Composable @ReadOnlyComposable get() = LocalAppShapes.current.FloatingActionButton
    val Chip @Composable @ReadOnlyComposable get() = LocalAppShapes.current.Chip
    val Card @Composable @ReadOnlyComposable get() = LocalAppShapes.current.Card
    val CardSmall @Composable @ReadOnlyComposable get() = LocalAppShapes.current.CardSmall
    val CardLarge @Composable @ReadOnlyComposable get() = LocalAppShapes.current.CardLarge
    val Dialog @Composable @ReadOnlyComposable get() = LocalAppShapes.current.Dialog
    val BottomSheet @Composable @ReadOnlyComposable get() = LocalAppShapes.current.BottomSheet
    val Modal @Composable @ReadOnlyComposable get() = LocalAppShapes.current.Modal
    val StopMarker @Composable @ReadOnlyComposable get() = LocalAppShapes.current.StopMarker
    val ListItem @Composable @ReadOnlyComposable get() = LocalAppShapes.current.ListItem
    val QRContainer @Composable @ReadOnlyComposable get() = LocalAppShapes.current.QRContainer
    val BusMarker @Composable @ReadOnlyComposable get() = LocalAppShapes.current.BusMarker
    val RouteIndicator @Composable @ReadOnlyComposable get() = LocalAppShapes.current.RouteIndicator
    val StatusBadge @Composable @ReadOnlyComposable get() = LocalAppShapes.current.StatusBadge
    val MapButton @Composable @ReadOnlyComposable get() = LocalAppShapes.current.MapButton
    val Avatar @Composable @ReadOnlyComposable get() = LocalAppShapes.current.Avatar
}
