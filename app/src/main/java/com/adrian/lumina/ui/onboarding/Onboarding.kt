package com.adrian.lumina.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adrian.lumina.R
import com.adrian.lumina.ui.components.LuminaButton
import com.adrian.lumina.ui.theme.LocalAnimationsEnabled
import com.adrian.lumina.ui.theme.Springs
import com.adrian.lumina.ui.theme.SquircleShape

/**
 * Primera pantalla (permiso de acceso). Un resplandor radial de acento (el único degradado de la app, sutil y detrás
 * de todo) ilumina la parte alta; el título y las tres promesas entran escalonados con un muelle.
 */
@Composable
fun Onboarding(onGrant: () -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val animate = LocalAnimationsEnabled.current
    var shown by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { shown = true }
    Box(modifier.fillMaxSize().background(c.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(c.primary.copy(alpha = 0.30f), c.primary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f), radius = size.width * 1.1f,
                ),
            )
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Spacer(Modifier.height(56.dp))
            Rise(shown, 0) {
                Box(Modifier.size(72.dp).clip(SquircleShape(20.dp)).background(c.primary), contentAlignment = Alignment.Center) {
                    Aperture(c.onPrimary)
                }
            }
            Spacer(Modifier.height(28.dp))
            Rise(shown, 1) { Text(stringResource(R.string.onb_title), style = MaterialTheme.typography.headlineLarge) }
            Spacer(Modifier.height(36.dp))
            Rise(shown, 2) { Feature(Icons.Outlined.Lock, stringResource(R.string.onb_1_t), stringResource(R.string.onb_1)) }
            Spacer(Modifier.height(22.dp))
            Rise(shown, 3) { Feature(Icons.Outlined.AutoAwesomeMosaic, stringResource(R.string.onb_2_t), stringResource(R.string.onb_2)) }
            Spacer(Modifier.height(22.dp))
            Rise(shown, 4) { Feature(Icons.Outlined.Tune, stringResource(R.string.onb_3_t), stringResource(R.string.onb_3)) }
            Box(Modifier.weight(1f))
            Rise(shown, 5) {
                Column {
                    LuminaButton(stringResource(R.string.permission_action), onGrant, Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.permission_body), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextButton(onClick = onSettings) { Text(stringResource(R.string.library_permissions), style = MaterialTheme.typography.labelLarge, color = c.primary) }
                }
            }
        }
    }
}

/** Entrada escalonada: cada bloque sube y aparece con un retardo de 70 ms por posición. */
@Composable
private fun Rise(visible: Boolean, order: Int, content: @Composable () -> Unit) {
    val animate = LocalAnimationsEnabled.current
    val p by animateFloatAsState(
        if (visible) 1f else 0f,
        if (animate) androidx.compose.animation.core.spring(0.8f, 180f) else androidx.compose.animation.core.snap(delayMillis = 0),
        label = "rise$order",
    )
    val delayed by produceDelay(visible, order * 70L)
    val v = if (delayed) p else 0f
    Box(Modifier.graphicsLayer { alpha = v; translationY = (1f - v) * 40.dp.toPx() }) { content() }
}

@Composable
private fun produceDelay(visible: Boolean, ms: Long) = androidx.compose.runtime.produceState(!visible || ms == 0L, visible) {
    if (visible && ms > 0) { kotlinx.coroutines.delay(ms); value = true }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(48.dp).clip(SquircleShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Marca propia: obturador de seis láminas dibujado con trazos finos. */
@Composable
private fun Aperture(color: Color) {
    Canvas(Modifier.size(40.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.width / 2 - 2.dp.toPx()
        drawCircle(color, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2.dp.toPx()))
        for (i in 0 until 6) {
            val a = Math.toRadians(60.0 * i + 15)
            val b = Math.toRadians(60.0 * i + 75)
            drawLine(
                color, Offset(c.x + (r * 0.95f) * Math.cos(a).toFloat(), c.y + (r * 0.95f) * Math.sin(a).toFloat()),
                Offset(c.x + (r * 0.35f) * Math.cos(b).toFloat(), c.y + (r * 0.35f) * Math.sin(b).toFloat()),
                strokeWidth = 2.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}
