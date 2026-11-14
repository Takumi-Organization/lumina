package com.adrian.lumina.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.adrian.lumina.domain.model.AppSettings
import com.adrian.lumina.domain.model.MediaAccess
import com.adrian.lumina.domain.model.mediaAccess
import com.adrian.lumina.domain.model.ThemeMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
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
import com.adrian.lumina.ui.LuminaHost
import com.adrian.lumina.ui.components.EmptyState
import com.adrian.lumina.ui.theme.LuminaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LuminaApp).container
        setContent {
            val s by container.settings.settings.collectAsState(initial = AppSettings())
            val dark = when (s.theme) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
            // Iconos de las barras del sistema acordes al tema elegido (no solo al del sistema).
            DisposableEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t),
                )
                onDispose {}
            }
            // «Proteger pantalla»: oculta en recientes y bloquea capturas.
            LaunchedEffect(s.privacyScreen) {
                if (s.privacyScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            // Los ajustes del sistema también cuentan: sin animaciones si la escala del sistema es 0.
            val systemAnimations = remember { Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
            LuminaTheme(dark, s.animations && systemAnimations, s.haptics, s.thumbQuality.px, s.accent) {
                PermissionGate(onAccessChanged = { container.media.refresh() }) { access, requestFull ->
                    LuminaHost(container, s, access, requestFull)
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

/** Nivel de acceso real: completo, parcial (fotos seleccionadas / solo un tipo) o ninguno. */
private fun currentAccess(context: android.content.Context): MediaAccess {
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return mediaAccess(
        sdk = Build.VERSION.SDK_INT,
        images = granted(Manifest.permission.READ_MEDIA_IMAGES), video = granted(Manifest.permission.READ_MEDIA_VIDEO),
        userSelected = Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
        legacyRead = granted(Manifest.permission.READ_EXTERNAL_STORAGE),
    )
}

/** Abre la pantalla de permisos de Lumina en los ajustes del sistema (último recurso si Android ya no muestra el diálogo). */
fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * Pide los permisos y entrega el nivel de acceso a [content]. El nivel se recalcula al volver a la app (el usuario puede
 * haberlo cambiado en los ajustes del sistema) y, si cambia, se vuelve a leer MediaStore.
 */
@Composable
private fun PermissionGate(onAccessChanged: () -> Unit, content: @Composable (MediaAccess, () -> Unit) -> Unit) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(currentAccess(context)) }
    fun recheck() {
        val now = currentAccess(context)
        if (now != access) { android.util.Log.i("LuminaMedia", "Acceso a medios: $access -> $now"); access = now; onAccessChanged() }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val before = access
        recheck()
        // Si el sistema no mostró el diálogo (denegado antes) y el acceso sigue sin ser completo, se lleva al usuario a los ajustes.
        if (before == access && access != MediaAccess.FULL) openAppSettings(context)
    }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { recheck() }
    if (access != MediaAccess.NONE) content(access) { launcher.launch(mediaPermissions()) }
    else com.adrian.lumina.ui.onboarding.Onboarding({ launcher.launch(mediaPermissions()) }, { openAppSettings(context) })
}
