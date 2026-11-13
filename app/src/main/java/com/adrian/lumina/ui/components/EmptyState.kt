package com.adrian.lumina.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Ilustración propia: marco de fotografía vacío con un punto de luz. */
@Composable
fun EmptyState(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val line = MaterialTheme.colorScheme.outline
    val lumen = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(132.dp)) {
            val w = size.width
            val stroke = Stroke(width = 3f)
            drawRoundRect(line, Offset(w * .08f, w * .2f), Size(w * .84f, w * .6f),
                androidx.compose.ui.geometry.CornerRadius(14f, 14f), style = stroke)
            drawCircle(lumen, radius = w * .075f, center = Offset(w * .66f, w * .38f))
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .08f, w * .72f); lineTo(w * .38f, w * .46f); lineTo(w * .56f, w * .62f)
                lineTo(w * .7f, w * .52f); lineTo(w * .92f, w * .74f)
            }
            drawPath(path, line, style = stroke)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(10.dp)) { Text(actionLabel) }
        }
    }
}
