package com.adrian.lumina.ui.gallery

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.ui.res.stringResource
import com.adrian.lumina.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.ui.theme.LocalAnimationsEnabled

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryCell(
    item: MediaItem,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val animate = LocalAnimationsEnabled.current
    val target = if (selected) 0.86f else 1f
    val spec: AnimationSpec<Float> =
        if (animate) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium) else snap()
    val scale by animateFloatAsState(target, spec, label = "cellScale")
    val lumen = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .aspectRatio(1f)
            .testTag("cell-${item.id}")
            .semantics { this.selected = selected; contentDescription = item.name }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
            ),
    ) {
        Box(
            Modifier.fillMaxSize().scale(scale)
                .clip(RoundedCornerShape(if (selected) 8.dp else 0.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (selected) Modifier.border(2.dp, lumen, RoundedCornerShape(8.dp)) else Modifier),
        ) {
            AsyncImage(
                model = Thumb(item.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.isVideo) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(4.dp)
                        .clip(RoundedCornerShape(50)).background(Color(0x99000000))
                        .padding(start = 3.dp, end = 7.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(formatDuration(item.durationMs), color = Color.White, fontSize = 11.sp)
                }
            }
            if (item.isFavorite) {
                Icon(
                    Icons.Rounded.Favorite, stringResource(R.string.favorite), tint = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).size(15.dp),
                )
            }
        }
        // Marca de selección: círculo vacío visible en modo selección, relleno ámbar al elegir.
        if (selecting) {
            Box(
                Modifier.align(Alignment.TopStart).padding(7.dp).size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) lumen else Color(0x55000000))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            }
        }
    }
}
