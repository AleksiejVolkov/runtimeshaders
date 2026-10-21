package com.offmind.runtimeshaders.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import com.offmind.runtimeshaders.ui.components.AppAppearanceTheme
import com.offmind.runtimeshaders.ui.components.AppBackground
import com.offmind.runtimeshaders.ui.components.AppIconButton
import com.offmind.runtimeshaders.ui.components.AppMenuRow
import com.offmind.runtimeshaders.ui.components.AppScreen
import com.offmind.runtimeshaders.ui.theme.AppAppearance

@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onChooseBackEffect: () -> Unit,
    onChooseAppearance: () -> Unit,
    selectedAppearance: AppAppearance,
    selectedBackEffect: PredictiveBackEffect,
) {
    SettingsSurface("Settings", onBack) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                AppMenuRow(
                    title = "Appearance",
                    description = "Choose the style of the app interface",
                    detail = selectedAppearance.title,
                    onClick = onChooseAppearance,
                )
            }
            item {
                AppMenuRow(
                    title = "Choose back effect",
                    description = "Select the shader used for predictive back",
                    detail = selectedBackEffect.title,
                    onClick = onChooseBackEffect,
                )
            }
        }
    }
}

@Composable
internal fun BackEffectPickerScreen(
    selectedEffect: PredictiveBackEffect,
    onBack: () -> Unit,
    onEffectSelected: (PredictiveBackEffect) -> Unit,
) {
    SettingsSurface("Choose back effect", onBack) {
        LazyColumn(
            modifier = Modifier.selectableGroup(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(PredictiveBackEffect.entriesList, key = { it.id }) { effect ->
                AppMenuRow(
                    title = effect.title,
                    description = effect.description,
                    selected = effect == selectedEffect,
                    onClick = { onEffectSelected(effect) },
                )
            }
        }
    }
}

@Composable
internal fun AppearancePickerScreen(
    selectedAppearance: AppAppearance,
    onBack: () -> Unit,
    onAppearanceSelected: (AppAppearance) -> Unit,
) {
    SettingsSurface("Appearance", onBack) {
        LazyColumn(
            modifier = Modifier.selectableGroup(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    "Applies to the catalog and settings. Glass and Neumorphism follow your device theme. " +
                        "Effect demos keep their own design.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(AppAppearance.entries, key = { it.id }) { appearance ->
                // Each option previews its own material, regardless of the active appearance.
                val selected = appearance == selectedAppearance
                val outline = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                AppAppearanceTheme(appearance) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                            .border(2.dp, outline, RoundedCornerShape(28.dp))
                    ) {
                        AppBackground(Modifier.matchParentSize())
                        AppMenuRow(
                            title = appearance.title,
                            description = appearance.description,
                            selected = selected,
                            onClick = { onAppearanceSelected(appearance) },
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSurface(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppScreen {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            content()
        }
    }
}
