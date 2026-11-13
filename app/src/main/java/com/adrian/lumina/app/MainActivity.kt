package com.adrian.lumina.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.adrian.lumina.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adrian.lumina.ui.components.EmptyState
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryViewModel
import com.adrian.lumina.ui.theme.LuminaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LuminaApp).container
        setContent {
            LuminaTheme {
                PermissionGate {
                    val vm: GalleryViewModel = viewModel(
                        factory = viewModelFactory { initializer { GalleryViewModel(container.media, container.favorites) } }
                    )
                    val state by vm.state.collectAsStateWithLifecycle()
                    val selection by vm.selection.collectAsStateWithLifecycle()
                    val columns by vm.columns.collectAsStateWithLifecycle()
                    val favoritesOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
                    GalleryScreen(
                        state = state, selection = selection, columns = columns, favoritesOnly = favoritesOnly,
                        allSelectedFavorite = selection.isNotEmpty() && vm.allSelectedAreFavorite(),
                        actions = GalleryActions(
                            onToggle = vm::toggle, onToggleDay = vm::toggleGroup,
                            onClearSelection = vm::clearSelection, onFavoriteSelected = vm::toggleFavoriteOfSelection,
                            onFavoritesOnly = vm::setFavoritesOnly, onColumns = vm::setColumns,
                        ),
                    )
                }
            }
        }
    }
}

private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/** Acceso completo o parcial (API 34 «fotos seleccionadas») cuentan como concedido; en parcial solo se ve lo elegido. */
private fun hasAccess(context: android.content.Context): Boolean {
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return when {
        Build.VERSION.SDK_INT >= 33 ->
            (granted(Manifest.permission.READ_MEDIA_IMAGES) || granted(Manifest.permission.READ_MEDIA_VIDEO)) ||
                (Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
        else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(hasAccess(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        allowed = hasAccess(context)
    }
    if (allowed) content() else EmptyState(
        title = stringResource(R.string.permission_title),
        body = stringResource(R.string.permission_body),
        actionLabel = stringResource(R.string.permission_action),
        onAction = { launcher.launch(mediaPermissions()) },
    )
}
