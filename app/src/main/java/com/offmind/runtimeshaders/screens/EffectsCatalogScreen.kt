package com.offmind.runtimeshaders.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.ui.components.AppIconButton
import com.offmind.runtimeshaders.ui.components.AppMenuRow
import com.offmind.runtimeshaders.ui.components.AppScreen

@Composable
internal fun EffectsCatalogScreen(
    category: EffectCategory,
    ancestors: List<EffectCategory>,
    onItemSelected: (EffectCatalogItem) -> Unit,
    onSettingsSelected: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    AppScreen(showBranding = true) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            CatalogHeader(category, ancestors, onBack, onSettingsSelected)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "description") {
                    Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                        Text(category.description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(category.summary(), color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                items(category.children, key = { "entry:${it.id}" }) { item ->
                    AppMenuRow(
                        title = item.title,
                        description = item.description,
                        onClick = { onItemSelected(item) },
                        leadingIcon = if (item is EffectCategory) Icons.AutoMirrored.Filled.List else Icons.Filled.PlayArrow,
                        detail = (item as? EffectCategory)?.summary(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    category: EffectCategory,
    ancestors: List<EffectCategory>,
    onBack: (() -> Unit)?,
    onSettingsSelected: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (onBack != null) {
            AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        }
        Column(Modifier.weight(1f)) {
            if (ancestors.isNotEmpty()) {
                Text(
                    text = ancestors.joinToString(" / ") { it.title },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = category.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
        }
        AppIconButton(Icons.Filled.MoreVert, "Settings", onSettingsSelected)
    }
}

private fun EffectCategory.summary(): String {
    val groups = children.count { it is EffectCategory }
    val demos = "$effectCount ${if (effectCount == 1) "demo" else "demos"}"
    return if (groups == 0) demos else "$groups ${if (groups == 1) "collection" else "collections"} · $demos"
}
