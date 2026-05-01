package com.vibra.bus.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.google_icon

/**
 * Secondary Glass Button with Material 3 styling
 * Wrapper around GlassButton with secondary styling
 */
// SecondaryGlassButton.kt

@Composable
fun SecondaryGlassButton(
    text: String,
    onClick: () -> Unit,
    showRecommendedBadge: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.google_icon),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Badge DEBAJO del botón, como recuadro integrado
        if (showRecommendedBadge) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5C518).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFF5C518).copy(alpha = 0.6f))
            ) {
                Text(
                    text = "⭐ Recomendado",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF5C518)
                )
            }
        }
    }
}