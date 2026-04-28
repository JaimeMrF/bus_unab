package com.vibra.bus.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shapes system for VibraBus
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object VibraBusShapes {
    val ButtonPrimary = RoundedCornerShape(12.dp)
    val ButtonSecondary = RoundedCornerShape(10.dp)
    val InputField = RoundedCornerShape(12.dp)
    val FloatingActionButton = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(8.dp)
    
    val Card = RoundedCornerShape(16.dp)
    val CardSmall = RoundedCornerShape(12.dp)
    val CardLarge = RoundedCornerShape(20.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Modal = RoundedCornerShape(24.dp)

    val BusMarker = RoundedCornerShape(50.dp)
    val StopMarker = RoundedCornerShape(8.dp)
    val RouteIndicator = RoundedCornerShape(4.dp)
    val StatusBadge = RoundedCornerShape(20.dp)
    val ListItem = RoundedCornerShape(12.dp)
    val MapButton = RoundedCornerShape(50.dp)
    val QRContainer = RoundedCornerShape(20.dp)
    val OwlAvatar = RoundedCornerShape(50.dp)
}

object ShapeUtils {
    fun customRoundedCornerShape(
        topStart: Float = 0f,
        topEnd: Float = 0f,
        bottomEnd: Float = 0f,
        bottomStart: Float = 0f
    ) = RoundedCornerShape(
        topStart = topStart.dp,
        topEnd = topEnd.dp,
        bottomEnd = bottomEnd.dp,
        bottomStart = bottomStart.dp
    )
}
