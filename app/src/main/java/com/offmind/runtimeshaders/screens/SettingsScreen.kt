package com.offmind.runtimeshaders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect

@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onChooseBackEffect: () -> Unit
) {
    SettingsSurface {
        SettingsHeader(
            title = "Settings",
            onBack = onBack
        )
        SettingsRow(
            title = "Choose back effect",
            description = "Select the shader used for predictive back.",
            trailing = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.68f),
                    modifier = Modifier.size(22.dp)
                )
            },
            onClick = onChooseBackEffect
        )
    }
}

@Composable
internal fun BackEffectPickerScreen(
    selectedEffect: PredictiveBackEffect,
    onBack: () -> Unit,
    onEffectSelected: (PredictiveBackEffect) -> Unit
) {
    var currentSelectedEffect by remember(selectedEffect) {
        mutableStateOf(selectedEffect)
    }

    SettingsSurface {
        SettingsHeader(
            title = "Choose back effect",
            onBack = onBack
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PredictiveBackEffect.entriesList) { effect ->
                SettingsRow(
                    title = effect.title,
                    description = effect.description,
                    trailing = {
                        if (effect == currentSelectedEffect) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF8BD3FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    onClick = {
                        currentSelectedEffect = effect
                        onEffectSelected(effect)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSurface(
    content: @Composable ColumnScope.() -> Unit
) {
    val systemInsets = WindowInsets.systemBars.asPaddingValues()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF07080B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF05070C),
                            0.26f to Color(0xFF111827),
                            0.52f to Color(0xFF071923),
                            0.76f to Color(0xFF17131D),
                            1.00f to Color(0xFF030406)
                        )
                    )
                )
                .padding(
                    start = 16.dp,
                    top = systemInsets.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = systemInsets.calculateBottomPadding() + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Box(
            modifier = Modifier.padding(start = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            trailing()
        }
    }
}
