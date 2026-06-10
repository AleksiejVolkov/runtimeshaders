package com.offmind.runtimeshaders.navigation.predictive

import android.graphics.RenderEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform

/**
 * Broadcast to every NavDisplay entry so that the *outgoing* (top) entry can dissolve itself
 * with the predictive-back shader, while the real destination entry renders normally behind it.
 *
 * [progress] is already in the shader's internal range (0..[BACK_COMPLETE_PROGRESS]); the screen
 * is fully gone at the top of that range, which is reached exactly as the pop commits.
 */
internal data class PredictiveBackRenderState(
    val active: Boolean = false,
    val progress: Float = 0f,
    val touch: Offset = Offset.Zero,
    val edge: Int = 0,
    val effect: PredictiveBackEffect = PredictiveBackEffect.default,
    val outgoingKey: Any? = null,
)

internal val LocalPredictiveBackRender = compositionLocalOf { PredictiveBackRenderState() }

/**
 * Wraps an entry's content. When this entry is the one being dismissed, applies the predictive
 * dissolve shader to its own pixels; otherwise renders the content untouched (so the destination
 * entry keeps all of its real state — scroll position, text, etc.).
 */
@Composable
internal fun PredictiveBackDissolve(
    entryKey: Any,
    content: @Composable () -> Unit,
) {
    val render = LocalPredictiveBackRender.current
    val isOutgoing = render.active && render.progress > 0f && render.outgoingKey == entryKey

    val shader = remember(render.effect) {
        Shader(render.effect.shaderSource).getRuntimeShader(
            uniforms = listOf(
                Uniform(Uniform.Type.SHADER, "image"),
                Uniform(Uniform.Type.VEC2, "resolution"),
                Uniform(Uniform.Type.VEC2, "touch"),
                Uniform(Uniform.Type.FLOAT, "progress"),
                Uniform(Uniform.Type.FLOAT, "edge"),
                Uniform(Uniform.Type.FLOAT, "time"),
            ),
            customFunctions = setOf(ShaderFunction.CUBICOUT, ShaderFunction.HASH21),
        )
    }
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isOutgoing) {
        if (isOutgoing) {
            while (true) {
                frameTimeNanos = withFrameNanos { it }
            }
        } else {
            frameTimeNanos = 0L
        }
    }

    val dissolveModifier =
        if (isOutgoing) {
            Modifier.graphicsLayer {
                val centerX = if (render.touch.x.isInfinite()) size.width else render.touch.x
                compositingStrategy = CompositingStrategy.Offscreen
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("touch", centerX, render.touch.y)
                shader.setFloatUniform("progress", render.progress)
                shader.setFloatUniform("edge", render.edge.toFloat())
                shader.setFloatUniform("time", frameTimeNanos / 1_000_000_000f)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            }
        } else {
            Modifier
        }

    Box(modifier = dissolveModifier) {
        content()
    }
}
