package com.adrian.lumina.ui.viewer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import coil3.compose.AsyncImage
import com.adrian.lumina.R
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.usecase.SEEK_STEP_MS
import com.adrian.lumina.domain.usecase.formatClock
import com.adrian.lumina.domain.usecase.progressFraction
import com.adrian.lumina.domain.usecase.seekTarget
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem as ExoMediaItem

/** Estado observable del reproductor para Compose (se refresca por eventos y cada 200 ms para la posición). */
@Stable
class PlayerSnapshot {
    var isPlaying by mutableStateOf(false)
    var ended by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var volume by mutableFloatStateOf(1f)
    var aspect by mutableFloatStateOf(16f / 9f)

    fun sync(p: Player) {
        isPlaying = p.isPlaying
        ended = p.playbackState == Player.STATE_ENDED
        positionMs = p.currentPosition
        durationMs = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        volume = p.volume
        val v: VideoSize = p.videoSize
        if (v.width > 0 && v.height > 0) aspect = v.width * v.pixelWidthHeightRatio / v.height
    }
}

/** Crea el ExoPlayer mientras [enabled]; lo libera al salir y lo pausa al ir a segundo plano. */
@Composable
fun rememberExoPlayer(uri: String, enabled: Boolean): ExoPlayer? {
    val context = LocalContext.current
    var player by remember(uri) { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(uri, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val p = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                /* handleAudioFocus = */ true,
            )
            .build().apply {
                setMediaItem(ExoMediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_OFF
                prepare()
                playWhenReady = true
            }
        player = p
        onDispose { p.release(); player = null }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player?.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return player
}

@Composable
fun rememberPlayerSnapshot(player: Player): PlayerSnapshot {
    val snap = remember(player) { PlayerSnapshot() }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) = snap.sync(p)
        }
        player.addListener(listener)
        snap.sync(player)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) { while (true) { snap.sync(player); delay(200) } }
    return snap
}

/**
 * Página de vídeo: póster con botón de reproducir; al pulsarlo (o con autoplay) crea el player.
 * Un toque alterna los controles; doble toque a izquierda/derecha retrocede/avanza 10 s.
 */
@Composable
fun VideoPage(
    item: MediaItem,
    isCurrent: Boolean,
    autoPlay: Boolean,
    onTap: () -> Unit,
    onPlayerChange: (Player?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var started by remember(item.id, isCurrent) { mutableStateOf(autoPlay && isCurrent) }
    val player = rememberExoPlayer(item.uri, enabled = isCurrent && started)
    LaunchedEffect(player, isCurrent) { onPlayerChange(if (isCurrent) player else null) }

    Box(modifier.fillMaxSize()) {
        if (player == null) {
            AsyncImage(Thumb(item.uri, 1024), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) })
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color(0x99000000))
                        .clickable { started = true }.testTag("video-start"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PlayArrow, stringResource(R.string.play_video), tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
        } else {
            ActiveVideo(player, onTap)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ActiveVideo(player: ExoPlayer, onTap: () -> Unit) {
    val snap = rememberPlayerSnapshot(player)
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackLeft by remember { mutableStateOf(false) }
    LaunchedEffect(feedback) { if (feedback != null) { delay(700); feedback = null } }

    Box(
        Modifier.fillMaxSize().pointerInput(player) {
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = { at ->
                    val left = at.x < size.width / 2f
                    val delta = if (left) -SEEK_STEP_MS else SEEK_STEP_MS
                    player.seekTo(seekTarget(player.currentPosition, player.duration.coerceAtLeast(0), delta))
                    feedbackLeft = left
                    feedback = if (left) "−10 s" else "+10 s"
                },
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.aspectRatio(snap.aspect).fillMaxWidth().testTag("video-surface"),
        )
        feedback?.let {
            Box(Modifier.fillMaxSize().padding(horizontal = 48.dp), contentAlignment = if (feedbackLeft) Alignment.CenterStart else Alignment.CenterEnd) {
                Text(
                    it, color = Color.White, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("video-seek-feedback"),
                )
            }
        }
        if (snap.ended) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Color(0x99000000))
                    .clickable { player.seekTo(0); player.play() }.testTag("video-replay"),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Replay, stringResource(R.string.video_replay), tint = Color.White, modifier = Modifier.size(44.dp)) }
        }
    }
}

/** Controles propios: reproducir/pausa, progreso, volumen con silencio y girar a horizontal. */
@Composable
fun VideoControls(player: Player, modifier: Modifier = Modifier) {
    val snap = rememberPlayerSnapshot(player)
    val context = LocalContext.current
    val seekDescription = stringResource(R.string.video_seek_bar)
    val volumeDescription = stringResource(R.string.video_volume)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var lastVolume by remember { mutableFloatStateOf(1f) }
    val fraction = scrubbing ?: progressFraction(snap.positionMs, snap.durationMs)
    val shownPosition = scrubbing?.let { (it * snap.durationMs).toLong() } ?: snap.positionMs

    Column(modifier.fillMaxWidth().background(Color(0x8C000000)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatClock(shownPosition), color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("video-time"))
            Slider(
                value = fraction,
                onValueChange = { scrubbing = it },
                onValueChangeFinished = {
                    scrubbing?.let { player.seekTo((it * snap.durationMs).toLong()) }
                    scrubbing = null
                },
                enabled = snap.durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0x55FFFFFF),
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).testTag("video-seek")
                    .semantics { contentDescription = seekDescription },
            )
            Text(formatClock(snap.durationMs), color = Color(0xFFCFC8BC), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (snap.isPlaying) player.pause() else { if (snap.ended) player.seekTo(0); player.play() } },
                modifier = Modifier.testTag("video-play-pause").semantics { stateDescription = if (snap.isPlaying) "playing" else "paused" },
            ) {
                Icon(
                    if (snap.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    stringResource(if (snap.isPlaying) R.string.video_pause else R.string.video_play), tint = Color.White,
                )
            }
            IconButton(
                onClick = { if (snap.volume > 0f) { lastVolume = snap.volume; player.volume = 0f } else player.volume = lastVolume.coerceAtLeast(0.3f) },
                modifier = Modifier.testTag("video-mute"),
            ) {
                Icon(
                    if (snap.volume > 0f) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                    stringResource(if (snap.volume > 0f) R.string.video_mute else R.string.video_unmute), tint = Color.White,
                )
            }
            Slider(
                value = snap.volume, onValueChange = { player.volume = it },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color(0x55FFFFFF),
                ),
                modifier = Modifier.width(110.dp).testTag("video-volume")
                    .semantics { contentDescription = volumeDescription },
            )
            Box(Modifier.weight(1f))
            IconButton(
                onClick = {
                    (context as? Activity)?.requestedOrientation =
                        if (landscape) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                },
                modifier = Modifier.testTag("video-rotate"),
            ) { Icon(Icons.Outlined.ScreenRotation, stringResource(R.string.video_rotate), tint = Color.White) }
        }
    }
}
