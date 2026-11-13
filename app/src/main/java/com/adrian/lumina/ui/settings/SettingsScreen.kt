package com.adrian.lumina.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.AlbumSort
import com.adrian.lumina.domain.model.AppSettings
import com.adrian.lumina.domain.model.ThemeMode
import com.adrian.lumina.domain.model.ThumbQuality
import com.adrian.lumina.domain.usecase.formatBytes
import com.adrian.lumina.ui.albums.sortRes

@Composable
fun SettingsScreen(
    settings: AppSettings,
    storage: StorageUi?,
    version: String,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp).testTag("settings"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp))

        Header(R.string.set_appearance)
        Choice(R.string.set_theme, ThemeMode.entries, settings.theme, { stringResource(themeRes(it)) }, "theme") { v -> onChange { it.copy(theme = v) } }
        Toggle(R.string.set_animations, settings.animations, "animations") { v -> onChange { it.copy(animations = v) } }
        Toggle(R.string.set_haptics, settings.haptics, "haptics") { v -> onChange { it.copy(haptics = v) } }

        Header(R.string.set_library)
        Text(stringResource(R.string.set_columns, settings.defaultColumns))
        Slider(
            settings.defaultColumns.toFloat(), { v -> onChange { it.copy(defaultColumns = v.toInt()) } },
            valueRange = 2f..7f, steps = 4, modifier = Modifier.testTag("set-columns"),
        )
        Choice(R.string.set_quality, ThumbQuality.entries, settings.thumbQuality, { stringResource(qualityRes(it)) }, "quality") { v -> onChange { it.copy(thumbQuality = v) } }
        Choice(R.string.set_album_sort, AlbumSort.entries, settings.albumSort, { stringResource(sortRes(it)) }, "sort") { v -> onChange { it.copy(albumSort = v) } }
        Toggle(R.string.set_autoplay, settings.autoplayVideos, "autoplay") { v -> onChange { it.copy(autoplayVideos = v) } }

        Header(R.string.set_privacy)
        Toggle(R.string.set_privacy_screen, settings.privacyScreen, "privacy") { v -> onChange { it.copy(privacyScreen = v) } }
        Text(stringResource(R.string.set_privacy_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Header(R.string.set_storage)
        if (storage == null) Text(stringResource(R.string.set_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            val s = storage.summary
            Text(pluralStringResource(R.plurals.set_photos, s.photos, s.photos, formatBytes(s.photoBytes)), Modifier.testTag("storage-photos"))
            Text(pluralStringResource(R.plurals.set_videos, s.videos, s.videos, formatBytes(s.videoBytes)), Modifier.testTag("storage-videos"))
            Text(stringResource(R.string.set_total, formatBytes(s.totalBytes)), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.set_local_data, storage.favorites, storage.albums), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClearCache, modifier = Modifier.testTag("clear-cache")) { Text(stringResource(R.string.set_clear_cache)) }

        Header(R.string.set_about)
        Text("Lumina $version", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.set_about_body), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun themeRes(t: ThemeMode) = when (t) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }
private fun qualityRes(q: ThumbQuality) = when (q) { ThumbQuality.LOW -> R.string.quality_low; ThumbQuality.NORMAL -> R.string.quality_normal; ThumbQuality.HIGH -> R.string.quality_high }

@Composable
private fun Header(res: Int) =
    Text(stringResource(res), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))

@Composable
private fun Toggle(label: Int, value: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(value, onChange, modifier = Modifier.testTag("set-$tag"))
    }
}

@Composable
private fun <T> Choice(label: Int, options: List<T>, selected: T, text: @Composable (T) -> String, tag: String, onPick: (T) -> Unit) {
    Column {
        Text(stringResource(label))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o -> FilterChip(o == selected, { onPick(o) }, { Text(text(o)) }, modifier = Modifier.testTag("set-$tag-$o")) }
        }
    }
}
