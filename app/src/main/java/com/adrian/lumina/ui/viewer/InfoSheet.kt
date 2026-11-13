package com.adrian.lumina.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.ExifInfo
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.usecase.formatAperture
import com.adrian.lumina.domain.usecase.formatBytes
import com.adrian.lumina.domain.usecase.formatCoords
import com.adrian.lumina.domain.usecase.formatExposure
import com.adrian.lumina.domain.usecase.formatFocalLength
import com.adrian.lumina.domain.usecase.megapixels
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(
    item: MediaItem,
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    loadExif: suspend (MediaItem) -> ExifInfo?,
    onDismiss: () -> Unit,
) {
    var exif by remember(item.id, locationGranted) { mutableStateOf<ExifInfo?>(null) }
    LaunchedEffect(item.id, locationGranted) { exif = loadExif(item) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.testTag("info-sheet").navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            InfoRow(item.name, stringResource(R.string.info_name))
            InfoRow(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(item.dateMillis)), stringResource(R.string.info_date))
            if (item.width > 0) {
                val mp = megapixels(item.width, item.height)
                InfoRow(
                    stringResource(R.string.info_megapixels, item.width, item.height, String.format(Locale.getDefault(), "%.1f", mp ?: 0.0)),
                    stringResource(R.string.info_resolution),
                )
            }
            InfoRow(formatBytes(item.sizeBytes), stringResource(R.string.info_size))
            InfoRow(item.relativePath.ifBlank { item.bucketName }, stringResource(R.string.info_folder))
            exif?.camera?.let { InfoRow(it, stringResource(R.string.info_camera)) }
            exif?.let { e ->
                val parts = listOfNotNull(
                    e.aperture?.let(::formatAperture), e.exposureSeconds?.let(::formatExposure),
                    e.iso?.let { "ISO $it" }, e.focalLengthMm?.let(::formatFocalLength),
                )
                if (parts.isNotEmpty()) InfoRow(parts.joinToString("  ·  "), stringResource(R.string.info_exposure))
            }
            if (!item.isVideo) {
                val e = exif
                when {
                    e?.hasLocation == true -> {
                        val lat = e.latitude!!; val lon = e.longitude!!
                        InfoRow(
                            formatCoords(lat, lon), stringResource(R.string.info_location),
                            action = stringResource(R.string.info_open_map),
                            onAction = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                        )
                    }
                    !locationGranted -> InfoRow(
                        stringResource(R.string.info_location_allow), stringResource(R.string.info_location),
                        action = null, onAction = onRequestLocation, emphasize = true,
                    )
                    e != null -> InfoRow(stringResource(R.string.info_location_none), stringResource(R.string.info_location))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    value: String, label: String, action: String? = null, onAction: (() -> Unit)? = null, emphasize: Boolean = false,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp)
            .then(if (onAction != null && action == null) Modifier.clickable(onClick = onAction) else Modifier),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            value, style = MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (action != null && onAction != null) {
            Text(
                action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp).clickable(onClick = onAction),
            )
        }
    }
}
