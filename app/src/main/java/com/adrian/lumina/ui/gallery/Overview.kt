package com.adrian.lumina.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.adrian.lumina.R
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.usecase.MonthOverview
import com.adrian.lumina.domain.usecase.YearOverview
import com.adrian.lumina.ui.theme.SquircleShape
import java.time.format.TextStyle
import java.util.Locale

/** Nivel «años»: una portada grande por año con el número gigante encima, como un cartel. */
@Composable
fun YearsView(years: List<YearOverview>, pad: PaddingValues, onYear: (YearOverview) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().navigationBarsPadding(), contentPadding = pad, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(years, key = { it.year }) { y ->
            Box(
                Modifier.padding(horizontal = 14.dp).fillMaxWidth().aspectRatio(1.35f).clip(SquircleShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { onYear(y) }.testTag("year-${y.year}"),
            ) {
                AsyncImage(Thumb(y.cover.uri, 640), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // Velo plano (no degradado) para que el número se lea sobre cualquier foto.
                Box(Modifier.fillMaxSize().background(Color(0x59000000)))
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text("${y.year}", style = MaterialTheme.typography.displayLarge, color = Color.White)
                    Text(
                        pluralStringResource(R.plurals.items_count, y.count, y.count) + " · " + y.months.size,
                        style = MaterialTheme.typography.labelMedium, color = Color(0xCCFFFFFF),
                    )
                }
            }
        }
    }
}

/** Nivel «meses»: rejilla de dos columnas; cada mes con su portada, nombre y recuento. */
@Composable
fun MonthsView(years: List<YearOverview>, pad: PaddingValues, onMonth: (MonthOverview) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        GridCells.Fixed(2), modifier.fillMaxSize().navigationBarsPadding(), contentPadding = pad,
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        years.forEach { y ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "y-${y.year}") {
                Text("${y.year}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 16.dp, top = 14.dp))
            }
            items(y.months, key = { "m-${it.month}" }) { m ->
                val locale = Locale.getDefault()
                Box(
                    Modifier.padding(horizontal = if (false) 0.dp else 0.dp).fillMaxWidth().aspectRatio(1f).clip(SquircleShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).clickable { onMonth(m) }.testTag("month-${m.month}"),
                ) {
                    AsyncImage(Thumb(m.cover.uri, 320), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Row(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x73000000)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            m.month.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) },
                            style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1,
                        )
                        Text("${m.count}", style = MaterialTheme.typography.labelMedium, color = Color(0xCCFFFFFF))
                    }
                }
            }
        }
    }
}
