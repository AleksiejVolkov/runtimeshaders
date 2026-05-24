package com.offmind.runtimeshaders.navigation.predictive

import android.graphics.RenderEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform

@Composable
internal fun PredictiveBackShaderLayer(
    state: PredictiveBackShaderState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shader = remember {
        Shader(predictiveBackHumpMaskShader).getRuntimeShader(
            uniforms = listOf(
                Uniform(Uniform.Type.SHADER, "image"),
                Uniform(Uniform.Type.VEC2, "resolution"),
                Uniform(Uniform.Type.VEC2, "touch"),
                Uniform(Uniform.Type.FLOAT, "progress"),
                Uniform(Uniform.Type.FLOAT, "edge")
            ),
            customFunctions = setOf(
                ShaderFunction.CUBICOUT,
                ShaderFunction.HASH21
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            if (state.progress > 0f) {
                val centerX = if (state.touch.x.isInfinite()) size.width else state.touch.x
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = 1f - ((state.progress - 1f) / (BACK_COMPLETE_PROGRESS - 1f))
                    .coerceIn(0f, 1f)
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("touch", centerX, state.touch.y)
                shader.setFloatUniform("progress", state.progress)
                shader.setFloatUniform("edge", state.swipeEdge.toFloat())
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            } else {
                alpha = 1f
                renderEffect = null
            }
        }
    ) {
        content()
    }
}
