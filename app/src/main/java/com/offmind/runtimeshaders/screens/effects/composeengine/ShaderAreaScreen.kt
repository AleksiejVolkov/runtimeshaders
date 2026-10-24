package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The same effect on a button or on the whole screen, as a brush or a layer effect. */
private enum class ShaderArea(val label: String) { Button("Button"), FullScreen("Full screen"), Off("Off") }

private enum class ShaderPath(val label: String, val code: String) {
    Brush("ShaderBrush", "drawBehind { drawRect(brush) }"),
    Effect(
        "RenderEffect",
        "graphicsLayer { renderEffect = RenderEffect.createRuntimeShaderEffect(shader, \"content\") }",
    ),
}

private val Octaves = listOf(1, 8, 32)

private val BaseGradient = Brush.verticalGradient(listOf(Color(0xFF2B3045), Color(0xFF171A29)))

@Composable
internal fun ShaderAreaScreen() {
    var area by rememberSaveable { mutableStateOf(ShaderArea.Button) }
    var path by rememberSaveable { mutableStateOf(ShaderPath.Brush) }
    var octaves by rememberSaveable { mutableStateOf(8) }
    val time = rememberInfiniteTransition(label = "glow")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val frames by rememberFrameSummary(
        "screen=shader-area variant=area=${area.name}/path=${path.name}/octaves=$octaves",
    )

    Box(Modifier.fillMaxSize()) {
        if (area == ShaderArea.FullScreen) {
            AreaEffect(path, octaves, time, Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().drawBehind { drawRect(BaseGradient) })
        }
        // The controls sit on top; in full-screen mode the effect covers every pixel underneath.
        EngineScreen(
            title = "How many pixels",
            subtitle = "The same shader on a button, or on every pixel of the screen.",
            background = Color.Transparent,
        ) {
            EngineChoice("Area", ShaderArea.entries, area, { it.label }) { area = it }
            EngineChoice("Path", ShaderPath.entries, path, { it.label }) { path = it }
            EngineChoice("Complexity (noise octaves)", Octaves, octaves, { "$it×" }) { octaves = it }
            Text(path.code, style = MonoSmall.copy(color = EngineDim))
            FrameReadout(frames)
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (area == ShaderArea.Button) {
                    AreaEffect(
                        path,
                        octaves,
                        time,
                        Modifier.width(280.dp).height(72.dp).clip(RoundedCornerShape(36.dp)),
                    ) { Text("Play", color = Color.White, fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun AreaEffect(
    path: ShaderPath,
    octaves: Int,
    time: State<Float>,
    modifier: Modifier,
    label: @Composable () -> Unit = {},
) {
    when (path) {
        ShaderPath.Brush -> {
            val shader = remember { newRuntimeShader(areaShaderSource(sampleContent = false)) }
            val brush = remember(shader) { ShaderBrush(shader) }
            Box(
                modifier.drawBehind {
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("time", time.value)
                    shader.setFloatUniform("octaves", octaves.toFloat())
                    drawRect(brush)
                },
                contentAlignment = Alignment.Center,
            ) { label() }
        }
        ShaderPath.Effect -> {
            val shader = remember { newRuntimeShader(areaShaderSource(sampleContent = true)) }
            Box(
                modifier
                    .graphicsLayer {
                        shader.setFloatUniform("size", size.width, size.height)
                        shader.setFloatUniform("time", time.value)
                        shader.setFloatUniform("octaves", octaves.toFloat())
                        // The effect copies the uniforms when it is created, so it is created anew.
                        renderEffect = RenderEffect
                            .createRuntimeShaderEffect(shader, "content")
                            .asComposeRenderEffect()
                    }
                    .drawBehind { drawRect(BaseGradient) },
                contentAlignment = Alignment.Center,
            ) { label() }
        }
    }
}
