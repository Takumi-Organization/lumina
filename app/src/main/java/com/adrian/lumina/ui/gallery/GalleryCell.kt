package com.adrian.lumina.ui.gallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.usecase.formatClock
import com.adrian.lumina.ui.components.LocalSharedScopes
import com.adrian.lumina.ui.components.mediaKey
import com.adrian.lumina.ui.theme.LocalAnimationsEnabled
import com.adrian.lumina.ui.theme.LocalHapticsEnabled
import com.adrian.lumina.ui.theme.LocalThumbSize
import com.adrian.lumina.ui.theme.Springs

/**
 * Celda del mosaico. Rellena el hueco que le da su fila. Al seleccionar se encoge con un muelle, gana esquinas más
 * redondeadas y una casilla circular de acento; favorita = corazón; vídeo = duración sobre cápsula oscura.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryCell(
    item: MediaItem,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbPx: Int = LocalThumbSize.current,
) {
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val animate = LocalAnimationsEnabled.current
    val scale by animateFloatAsState(if (selected) 0.86f else 1f, if (animate) Springs.bouncy() else snap(), label = "cellScale")
    val corner by animateFloatAsState(if (selected) 14f else 5f, if (animate) Springs.snappy() else snap(), label = "cellCorner")
    val accent = MaterialTheme.colorScheme.primary
    val scopes = LocalSharedScopes.current
    val shared = if (scopes != null) with(scopes.shared) {
        Modifier.sharedBounds(rememberSharedContentState(mediaKey(item.id)), scopes.visibility)
    } else Modifier
    Box(
        modifier
            .then(shared)
            .testTag("cell-${item.id}")
            .semantics { this.selected = selected; contentDescription = item.name }
            .combinedClickable(
                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }, indication = null,
                onClick = {
                    if (hapticsOn && selecting) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = { if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
            ),
    ) {
        Box(
            Modifier.fillMaxSize().scale(scale).clip(RoundedCornerShape(corner.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = Thumb(item.uri, thumbPx),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { com.adrian.lumina.app.PerfLog.mark("primera miniatura") },
            )
            if (item.isVideo) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(6.dp).clip(CircleShape).background(Color(0x99000000))
                        .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(formatClock(item.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (item.isFavorite) {
                Icon(
                    Icons.Filled.Favorite, null, tint = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).size(15.dp),
                )
            }
        }
        // Casilla circular: hueca en modo selección, rellena de acento (con un rebote) al elegir.
        if (selecting) {
            val pop by animateFloatAsState(if (selected) 1f else 0.9f, if (animate) Springs.bouncy() else snap(), label = "checkPop")
            Box(
                Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp).scale(pop)
                    .clip(CircleShape).background(if (selected) accent else Color(0x55000000))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            }
        }
    }
}
