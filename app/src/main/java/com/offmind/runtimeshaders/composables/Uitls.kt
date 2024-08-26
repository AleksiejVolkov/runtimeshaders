package com.offmind.runtimeshaders.composables

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import kotlinx.coroutines.delay

@Composable
fun provideTimeAsState(initialValue: Float = 0f): State<Float> {
    val timeState = remember { mutableFloatStateOf(initialValue) }

    LaunchedEffect(Unit) {
        while (true) {
            timeState.floatValue += 0.01f
            delay(10)
        }
    }

    return timeState
}

@Composable
fun ShadedBox(
    modifier: Modifier = Modifier,
    shader: RuntimeShader,
    shaderUniforms: Map<String, ShaderTypedValue> = emptyMap(),
    includeTime: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    if (includeTime) {
        val timeState = provideTimeAsState()
        shader.setFloatUniform("time", timeState.value)
        LaunchedEffect(timeState) {
            shader.setFloatUniform("time", timeState.value)
        }
    }

    Box(modifier = modifier
        .onSizeChanged { size ->
            shader.setFloatUniform(
                "resolution",
                size.width.toFloat(),
                size.height.toFloat()
            )
        }
        .graphicsLayer {
            applyShaderProperties(shader, shaderUniforms)
            this.renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "image")
                .asComposeRenderEffect()
        }) {
        content()
    }
}

private fun applyShaderProperties(
    shader: RuntimeShader,
    properties: Map<String, ShaderTypedValue>
) {
    properties.forEach { (name, value) ->
        when (value) {
            is ShaderTypedValue.FloatType -> {
                shader.setFloatUniform(name, value.value)
            }

            is ShaderTypedValue.Vec2Type -> {
                shader.setFloatUniform(name, value.value1, value.value2)
            }

            is ShaderTypedValue.Vec3Type -> {
                shader.setFloatUniform(name, value.value1, value.value2, value.value3)
            }

            is ShaderTypedValue.Vec4Type -> {
                shader.setFloatUniform(
                    name,
                    value.value1,
                    value.value2,
                    value.value3,
                    value.value4
                )
            }
        }
    }
}