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
            LuminaTheme(dark, s.animations && systemAnimations, s.haptics, s.thumbQuality.px) { PermissionGate { LuminaHost(container, s) } }
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
