package com.adrian.lumina.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.adrian.lumina.ui.theme.LocalDark
import com.adrian.lumina.ui.theme.Springs
import com.adrian.lumina.ui.theme.motion
import com.adrian.lumina.ui.theme.SquircleShape

/** Línea fina de 0,5-1 dp (separador de listas, estilo iOS). */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outline) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** Color de una capa translúcida (barras flotantes): el fondo con ~82 % de opacidad. */
@Composable
fun glass(): Color = MaterialTheme.colorScheme.surface.copy(alpha = if (LocalDark.current) 0.86f else 0.9f)

/**
 * Barra de pestañas flotante: cápsula continua con sombra suave sobre el contenido, iconos de trazo fino, etiqueta bajo
 * el elegido y un indicador de acento que se desliza con un muelle.
 */
@Composable
fun LuminaTabs(
    labels: List<String>, icons: List<ImageVector>, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, tags: List<String> = emptyList(),
) {
    val c = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        BoxWithConstraints(
            Modifier.fillMaxWidth()
                .shadow(24.dp, SquircleShape(26.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(SquircleShape(26.dp)).background(glass()).padding(6.dp),
        ) {
            val slot = maxWidth / labels.size
            val x by animateDpAsState(slot * selected, motion(Springs.snappy()), label = "tabX")
            Box(Modifier.offset(x = x).width(slot).height(54.dp).clip(SquircleShape(20.dp)).background(c.primary.copy(alpha = 0.16f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEachIndexed { i, label ->
                    val on = i == selected
                    val tint by animateColorAsState(if (on) c.primary else c.onSurfaceVariant, motion(Springs.snappy()), label = "tabTint")
                    val scale by animateFloatAsState(if (on) 1f else 0.94f, motion(Springs.bouncy()), label = "tabScale")
                    Column(
                        Modifier.weight(1f).height(54.dp)
                            .let { if (tags.isNotEmpty()) it.testTag(tags[i]) else it }
                            .selectable(on, interactionSource = remember2(), indication = null, role = Role.Tab, onClick = { onSelect(i) }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(icons[i], null, tint = tint, modifier = Modifier.size(24.dp).scale2(scale))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1, overflow = TextOverflow.Clip, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable private fun remember2(): MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
private fun Modifier.scale2(s: Float): Modifier = this.scale(s)

/** Título grande de pantalla, estilo iOS: peso 700 a 34 sp, con el recuento en gris y acciones a la derecha. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, meta: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (meta != null) Text(
            meta, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
        )
        Box(Modifier.weight(1f))
        trailing()
    }
}

/** Botón de acento de esquinas continuas, con rebote al pulsar. */
@Composable
fun LuminaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    val c = MaterialTheme.colorScheme
    val source = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, motion(Springs.bouncy()), label = "btn")
    Box(
        modifier.scale2(scale).clip(SquircleShape(16.dp))
            .background(if (filled) c.primary else c.primary.copy(alpha = 0.14f))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (filled) c.onPrimary else c.primary)
    }
}



/** Píldora de selección (filtros, pestañas del editor): se rellena de acento con un muelle al elegirla. */
@Composable
fun LuminaPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) c.primary else c.surfaceVariant, motion(Springs.snappy()), label = "pill")
    Box(
        modifier.clip(androidx.compose.foundation.shape.CircleShape).background(bg)
            .selectable(selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPrimary else c.onBackground) }
}
