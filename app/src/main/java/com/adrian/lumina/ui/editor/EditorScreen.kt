package com.adrian.lumina.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adrian.lumina.R
import com.adrian.lumina.editor.Adjustment
import com.adrian.lumina.ui.albums.ConfirmDialog

private fun label(a: Adjustment) = when (a) {
    Adjustment.EXPOSURE -> R.string.adj_exposure
    Adjustment.CONTRAST -> R.string.adj_contrast
    Adjustment.SATURATION -> R.string.adj_saturation
    Adjustment.WARMTH -> R.string.adj_warmth
    Adjustment.SHADOWS -> R.string.adj_shadows
    Adjustment.HIGHLIGHTS -> R.string.adj_highlights
    Adjustment.SHARPEN -> R.string.adj_sharpen
    Adjustment.VIGNETTE -> R.string.adj_vignette
}

private val RATIOS = listOf(
    R.string.crop_original to null, R.string.crop_square to 1f, R.string.crop_4_3 to 4f / 3f,
    R.string.crop_3_4 to 3f / 4f, R.string.crop_16_9 to 16f / 9f, R.string.crop_9_16 to 9f / 16f,
)

@Composable
fun EditorScreen(vm: EditorViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.live.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val original by vm.original.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    val save by vm.saveState.collectAsStateWithLifecycle()
    val failed by vm.failedToLoad.collectAsStateWithLifecycle()
    var comparing by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var adjustment by rememberSaveable { mutableStateOf(Adjustment.EXPOSURE) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val close = { vm.release(); onClose() }

    BackHandler { if (vm.hasChanges) confirmDiscard = true else close() }
    LaunchedEffect(save) {
        when (save) {
            is SaveState.Done -> { Toast.makeText(context, R.string.editor_saved, Toast.LENGTH_SHORT).show(); vm.acknowledgeSave(); close() }
            SaveState.Failed -> { Toast.makeText(context, R.string.editor_save_failed, Toast.LENGTH_LONG).show(); vm.acknowledgeSave() }
            else -> {}
        }
    }

    Column(modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (vm.hasChanges) confirmDiscard = true else close() }, modifier = Modifier.testTag("editor-close")) {
                Icon(Icons.Rounded.Close, stringResource(R.string.back), tint = Color.White)
            }
            Box(Modifier.weight(1f))
            IconButton(vm::undo, enabled = canUndo, modifier = Modifier.testTag("editor-undo")) {
                Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.editor_undo), tint = if (canUndo) Color.White else Color.DarkGray)
            }
            IconButton(vm::redo, enabled = canRedo, modifier = Modifier.testTag("editor-redo")) {
                Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.editor_redo), tint = if (canRedo) Color.White else Color.DarkGray)
            }
            TextButton(vm::reset, enabled = !state.isIdentity) { Text(stringResource(R.string.editor_reset)) }
            if (save == SaveState.Saving) CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).testTag("editor-saving"), strokeWidth = 2.dp)
            else TextButton(vm::save, enabled = !state.isIdentity, modifier = Modifier.testTag("editor-save")) {
                Icon(Icons.Rounded.Check, null, Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.editor_save_copy))
            }
        }

        // Mantener pulsada la imagen muestra el original (comparar).
        Box(
            Modifier.weight(1f).fillMaxWidth().testTag("editor-preview")
                .pointerInput(Unit) { detectTapGestures(onPress = { comparing = true; tryAwaitRelease(); comparing = false }) },
            contentAlignment = Alignment.Center,
        ) {
            val shown = if (comparing) original else preview
            when {
                failed -> Text(stringResource(R.string.editor_load_failed), color = Color.White)
                shown == null -> CircularProgressIndicator(strokeWidth = 2.dp)
                else -> Image(shown.asImageBitmap(), null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
            }
            if (comparing) Text(
                stringResource(R.string.editor_original), color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            stringResource(R.string.editor_compare_hint), style = MaterialTheme.typography.labelMedium, color = Color.Gray,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        TabRow(tab, containerColor = Color.Black) {
            Tab(tab == 0, { tab = 0 }, Modifier.testTag("editor-tab-adjust")) { Text(stringResource(R.string.editor_tab_adjust), Modifier.padding(12.dp)) }
            Tab(tab == 1, { tab = 1 }, Modifier.testTag("editor-tab-crop")) { Text(stringResource(R.string.editor_tab_crop), Modifier.padding(12.dp)) }
        }
        if (tab == 0) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Adjustment.entries.forEach { a ->
                    FilterChip(adjustment == a, { adjustment = a }, { Text(stringResource(label(a))) }, modifier = Modifier.testTag("adj-${a.name}"))
                }
            }
            val value = adjustment.get(state)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = value, onValueChange = { vm.adjust(adjustment, it) }, onValueChangeFinished = vm::commitDraft,
                    valueRange = adjustment.min..1f, modifier = Modifier.weight(1f).testTag("editor-slider"),
                )
                Text("${(value * 100).toInt()}", Modifier.padding(start = 12.dp), color = Color.White)
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RATIOS.forEach { (res, ratio) -> FilterChip(false, { vm.cropRatio(ratio) }, { Text(stringResource(res)) }) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton({ vm.rotate(-1) }, modifier = Modifier.testTag("editor-rotate-left")) { Icon(Icons.Rounded.RotateLeft, stringResource(R.string.editor_rotate_left), tint = Color.White) }
                IconButton({ vm.rotate(1) }, modifier = Modifier.testTag("editor-rotate-right")) { Icon(Icons.Rounded.RotateRight, stringResource(R.string.editor_rotate_right), tint = Color.White) }
                IconButton(vm::flip, modifier = Modifier.testTag("editor-flip")) { Icon(Icons.Rounded.Flip, stringResource(R.string.editor_flip), tint = Color.White) }
            }
        }
    }

    if (confirmDiscard) ConfirmDialog(
        stringResource(R.string.editor_discard_title), stringResource(R.string.editor_discard_body),
        { confirmDiscard = false }, confirmLabel = stringResource(R.string.editor_discard),
    ) { confirmDiscard = false; close() }
}
