package com.adrian.lumina.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.adrian.lumina.ui.theme.LocalAnimationsEnabled
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

/** Marcadores de posición con pulso de opacidad (sin degradados). */
@Composable
fun GridSkeleton(columns: Int, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val animate = LocalAnimationsEnabled.current
    val pulse = if (animate) {
        val t = rememberInfiniteTransition(label = "skeleton")
        val a by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
        a
    } else 0.5f
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        userScrollEnabled = false,
    ) {
        items(columns * 14) {
            Box(
                Modifier.aspectRatio(1f).alpha(pulse).background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
