package com.offmind.runtimeshaders.screens

import android.graphics.RuntimeShader
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
        color = Color(0xFF0A0D13)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsAnimatedBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
}

@Composable
private fun SettingsAnimatedBackground() {
    val shader = remember { RuntimeShader(settingsBackgroundShader) }
    val paint = remember { Paint() }
    var frameTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            frameTimeNanos = withFrameNanos { it }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("time", frameTimeNanos / 1_000_000_000f)
                paint.asFrameworkPaint().shader = shader
                drawIntoCanvas { canvas ->
                    canvas.drawRect(
                        androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                        paint
                    )
                }
            }
    )
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
            .background(Color.White.copy(alpha = 0.16f))
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

private val settingsBackgroundShader = """
    uniform float2 resolution;
    uniform float time;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        float2 p = uv - 0.5;
        p.x *= resolution.x / resolution.y;

        float slow = time * 0.13;
        float breath = 0.5 + 0.5 * sin(time * 0.36);
        float diagonal = smoothstep(-0.75, 0.92, p.x * 0.62 + p.y + sin(slow) * 0.08);
        float vertical = smoothstep(0.0, 1.0, uv.y);

        vec3 ink = vec3(0.030, 0.038, 0.060);
        vec3 blue = vec3(0.055, 0.150, 0.230);
        vec3 plum = vec3(0.170, 0.090, 0.180);
        vec3 teal = vec3(0.050, 0.220, 0.230);
        vec3 violet = vec3(0.140, 0.120, 0.270);

        vec3 color = mix(ink, blue, smoothstep(0.0, 0.72, vertical + breath * 0.12));
        color = mix(color, plum, diagonal * 0.48);

        float2 glowA = p - vec2(-0.36 + sin(slow) * 0.05, -0.22 + cos(slow * 1.3) * 0.04);
        float2 glowB = p - vec2(0.42 + cos(slow * 0.9) * 0.04, 0.16 + sin(slow * 1.1) * 0.05);
        float lightA = exp(-dot(glowA, glowA) * 3.2);
        float lightB = exp(-dot(glowB, glowB) * 4.0);

        color += teal * lightA * (0.20 + breath * 0.08);
        color += violet * lightB * (0.24 + (1.0 - breath) * 0.08);

        float ripple = sin((p.x * 2.4 - p.y * 1.7) + time * 0.42) * 0.018;
        color += vec3(ripple, ripple * 0.65, ripple * 1.15);
        color = clamp(color, vec3(0.0), vec3(1.0));

        return half4(half3(color), 1.0);
    }
""".trimIndent()
