package com.ipanda.android.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val shimmerColors = listOf(
        Color(0xFF2A2A2A),
        Color(0xFF3A3A3A).copy(alpha = alpha),
        Color(0xFF2A2A2A)
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
    background(brush = brush, shape = RoundedCornerShape(8.dp))
}
