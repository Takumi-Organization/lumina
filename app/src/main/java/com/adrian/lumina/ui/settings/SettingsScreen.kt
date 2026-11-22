package com.adrian.lumina.ui.settings

import androidx.compose.animation.animateColorAsState
import com.adrian.lumina.ui.theme.TakumiCut
import com.adrian.lumina.ui.theme.TakumiSm
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.AccentChoice
import com.adrian.lumina.domain.model.AlbumSort
import com.adrian.lumina.domain.model.AppSettings
import com.adrian.lumina.domain.model.ThemeMode
import com.adrian.lumina.domain.model.ThumbQuality
import com.adrian.lumina.domain.usecase.formatBytes
import com.adrian.lumina.ui.albums.sortRes
import com.adrian.lumina.ui.components.Hairline
import com.adrian.lumina.ui.components.LuminaButton
import com.adrian.lumina.ui.components.ScreenTitle
import com.adrian.lumina.ui.theme.LocalDark
import com.adrian.lumina.ui.theme.Springs
import com.adrian.lumina.ui.theme.motion
import com.adrian.lumina.ui.theme.color

/** Ajustes con listas agrupadas de esquinas continuas, interruptores con muelle y acento elegible. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    storage: StorageUi?,
    version: String,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenTitle(stringResource(R.string.tab_settings))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).testTag("settings"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Section(R.string.set_appearance) {
                Row2 { Choice(R.string.set_theme, ThemeMode.entries, settings.theme, { stringResource(themeRes(it)) }, "theme") { v -> onChange { it.copy(theme = v) } } }
                Row2 { Toggle(R.string.set_animations, settings.animations, "animations") { v -> onChange { it.copy(animations = v) } } }
                Row2(last = true) { Toggle(R.string.set_haptics, settings.haptics, "haptics") { v -> onChange { it.copy(haptics = v) } } }
            }
            Section(R.string.set_library) {
                Row2 {
                    Column {
                        Text(stringResource(R.string.set_columns, settings.defaultColumns), style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            settings.defaultColumns.toFloat(), { v -> onChange { it.copy(defaultColumns = v.toInt()) } },
                            valueRange = 2f..7f, steps = 4, modifier = Modifier.testTag("set-columns"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
                            ),
                            thumb = { Box(Modifier.size(20.dp).clip(TakumiSm).background(MaterialTheme.colorScheme.primary)) },
                        )
                    }
                }
                Row2 { Choice(R.string.set_quality, ThumbQuality.entries, settings.thumbQuality, { stringResource(qualityRes(it)) }, "quality") { v -> onChange { it.copy(thumbQuality = v) } } }
                Row2 { Choice(R.string.set_album_sort, AlbumSort.entries, settings.albumSort, { stringResource(sortRes(it)) }, "sort") { v -> onChange { it.copy(albumSort = v) } } }
                Row2(last = true) { Toggle(R.string.set_autoplay, settings.autoplayVideos, "autoplay") { v -> onChange { it.copy(autoplayVideos = v) } } }
            }
            Section(R.string.set_privacy, footer = stringResource(R.string.set_privacy_note)) {
                Row2(last = true) { Toggle(R.string.set_privacy_screen, settings.privacyScreen, "privacy") { v -> onChange { it.copy(privacyScreen = v) } } }
            }
            Section(R.string.set_storage) {
                Row2 {
                    if (storage == null) Text(stringResource(R.string.set_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        val s = storage.summary
                        Column {
                            Text(formatBytes(s.totalBytes), style = MaterialTheme.typography.displayMedium)
                            Text(pluralStringResource(R.plurals.set_photos, s.photos, s.photos, formatBytes(s.photoBytes)), Modifier.testTag("storage-photos"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pluralStringResource(R.plurals.set_videos, s.videos, s.videos, formatBytes(s.videoBytes)), Modifier.testTag("storage-videos"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.set_local_data, storage.favorites, storage.albums), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                Row2(last = true) { LuminaButton(stringResource(R.string.set_clear_cache), onClearCache, Modifier.testTag("clear-cache"), filled = false) }
            }
            Section(R.string.set_about) {
                Row2(last = true) {
                    Column {
                        Text("Lumina $version", style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.set_about_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(120.dp)) // espacio para la barra flotante
        }
    }
}

private fun themeRes(t: ThemeMode) = when (t) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }
private fun qualityRes(q: ThumbQuality) = when (q) { ThumbQuality.LOW -> R.string.quality_low; ThumbQuality.NORMAL -> R.string.quality_normal; ThumbQuality.HIGH -> R.string.quality_high }

/** Sección agrupada: título pequeño en gris, tarjeta de esquinas continuas y pie opcional. */
@Composable
private fun Section(title: Int, footer: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 14.dp)) {
        Text(stringResource(title).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(TakumiCut(22.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, TakumiCut(22.dp))) { content() }
        if (footer != null) Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
    }
}

@Composable
private fun Row2(last: Boolean = false, content: @Composable () -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
        if (!last) Hairline(Modifier.padding(start = 16.dp))
    }
}

/** Interruptor tipo iOS: cápsula que se tiñe de acento y botón blanco con muelle. Toda la fila es el objetivo táctil. */
@Composable
private fun Toggle(label: Int, value: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    val track by animateColorAsState(if (value) c.primary else c.surfaceVariant, motion(Springs.snappy()), label = "track")
    val knob by animateDpAsState(if (value) 22.dp else 0.dp, motion(Springs.bouncy()), label = "knob")
    Row(
        Modifier.fillMaxWidth().toggleable(value, role = Role.Switch, onValueChange = onChange).testTag("set-$tag"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Box(Modifier.size(52.dp, 32.dp).clip(TakumiSm).background(track).border(1.dp, c.outline, TakumiSm).padding(3.dp)) {
            Box(Modifier.offset(x = knob).size(26.dp).clip(TakumiSm).background(if (value) c.onPrimary else c.onSurfaceVariant))
        }
    }
}

@Composable
private fun <T> Choice(label: Int, options: List<T>, selected: T, text: @Composable (T) -> String, tag: String, onPick: (T) -> Unit) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o -> Pill(text(o), o == selected, Modifier.testTag("set-$tag-$o")) { onPick(o) } }
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) c.primary else c.surfaceVariant, motion(Springs.snappy()), label = "pill")
    Box(
        modifier.clip(TakumiSm).background(bg).border(1.dp, if (selected) c.primary else c.outline, TakumiSm).selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPrimary else c.onBackground) }
}
