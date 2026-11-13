package com.adrian.lumina.ui.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.Album

/** Crear o editar un álbum (nombre obligatorio, descripción opcional). */
@Composable
fun AlbumFormDialog(initialName: String, initialDescription: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialName.isEmpty()) R.string.album_new else R.string.album_edit)) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.album_name)) }, modifier = Modifier.fillMaxWidth().testTag("album-name"))
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.album_description)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("album-description"))
            }
        },
        confirmButton = { TextButton({ onConfirm(name.trim(), description.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Elegir a qué álbum manual añadir la selección, o crear uno nuevo. */
@Composable
fun AlbumPickerDialog(albums: List<Album>, onDismiss: () -> Unit, onPick: (Long) -> Unit, onNew: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.album_add_to)) },
        text = {
            LazyColumn {
                items(albums, key = { it.id }) { a ->
                    Text(a.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().clickable { onPick(a.id) }.padding(vertical = 12.dp).testTag("pick-${a.id}"))
                }
            }
        },
        confirmButton = { TextButton(onNew) { Text(stringResource(R.string.album_new)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton(onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
