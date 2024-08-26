package com.offmind.runtimeshaders.screens.effects

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.offmind.runtimeshaders.composables.LoginForm
import com.offmind.runtimeshaders.composables.MemoryCard
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.fireShader
import kotlinx.coroutines.delay

@Composable
fun TestShaderScreen() {
    var percentage by remember { mutableFloatStateOf(0.0f) }
    var trigger by remember { mutableStateOf(false) }

    val shader = remember {
        Shader(fireShader).getRuntimeShader()
    }

    val percentageAnim = animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(4500, easing = FastOutLinearInEasing),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1D1D1D)),
        contentAlignment = Alignment.Center
    ) {
        ShadedBox(
            shader = shader,
            shaderUniforms = mapOf(
                "percentage" to ShaderTypedValue.FloatType(percentageAnim.value)
            ),
            includeTime = true
        ) {
            MemoryCard {
                percentage = 1f
            }
        }
    }
}