package com.adrian.lumina.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/** Ámbitos de transición compartida entre cuadrícula y visor. Nulo en pruebas sin transición. */
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedScopes(val shared: SharedTransitionScope, val visibility: AnimatedVisibilityScope)

val LocalSharedScopes = compositionLocalOf<SharedScopes?> { null }

fun mediaKey(id: Long) = "media-$id"

/**
 * Transición compartida miniatura ⇄ visor: los límites viajan con un muelle (ligeramente subamortiguado, así la foto
 * «aterriza» con un pequeño rebote) y el contenido se escala en vez de re-medirse en cada fotograma.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.mediaSharedBounds(id: Long, animationsEnabled: Boolean = true): Modifier {
    val scopes = LocalSharedScopes.current ?: return this
    return with(scopes.shared) {
        this@mediaSharedBounds.sharedBounds(
            rememberSharedContentState(mediaKey(id)), scopes.visibility,
            boundsTransform = BoundsTransform { _, _ -> if (animationsEnabled) spring(dampingRatio = 0.82f, stiffness = 330f) else spring(stiffness = 100_000f) },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.Center),
        )
    }
}
