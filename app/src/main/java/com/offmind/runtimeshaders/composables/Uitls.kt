package com.offmind.runtimeshaders.composables

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import kotlinx.coroutines.delay
import kotlin.collections.toFloatArray

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
    content: (@Composable () -> Unit)? = null
) {
    val timeState = provideTimeAsState()
    val paint = remember { Paint() }

    val shaderModifier = if (content == null) {
        modifier
            .onSizeChanged { size ->
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
            }
            .drawBehind {
                applyShaderProperties(shader, shaderUniforms)
                if (includeTime) shader.setFloatUniform("time", timeState.value)
                paint.asFrameworkPaint().shader = shader
                drawIntoCanvas { canvas ->
                    canvas.drawRect(
                        androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                        paint
                    )
                }
            }
    } else {
        modifier
            .onSizeChanged { size ->
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
            }
            .graphicsLayer {
                applyShaderProperties(shader, shaderUniforms)
                if (includeTime) shader.setFloatUniform("time", timeState.value)
                this.renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            }
    }

    Box(
        modifier = shaderModifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content?.invoke()
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

            is ShaderTypedValue.IntType -> {
                shader.setIntUniform(name, value.value)
            }

            is ShaderTypedValue.Vec2Array -> {
                shader.setVec2ArrayUniform(name, value.values)
            }
        }
    }
}

fun RuntimeShader.setVec2ArrayUniform(
    name: String,
    values: List<Pair<Float, Float>>,
    maxSize: Int = 10
) {
    require(values.size <= maxSize) {
        "Too many elements for uniform '$name'. Maximum allowed is $maxSize, but got ${values.size}"
    }

    val padded = values + List(maxSize - values.size) { 0f to 0f }
    val floatArray = padded.flatMap { listOf(it.first, it.second) }.toFloatArray()

    this.setFloatUniform(name, floatArray)
}