package com.offmind.runtimeshaders.screens.effects.lighting

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.removeUniform
import kotlin.math.sqrt

/**
 * Establishes a lighting scope. The [content] lambda receives a [LightingScopeReceiver],
 * making [LightingScopeReceiver.lightSource] and [LightingScopeReceiver.receivesLight]
 * available as plain modifier calls on any composable declared as an extension — exactly
 * like [androidx.compose.foundation.layout.RowScope.align] inside a Row.
 *
 * A global bloom shader is applied to the composited scene so light halos from sources
 * bleed across the full scope area, including transparent gaps between elements.
 */
@Composable
fun LightingScope(
    modifier: Modifier = Modifier,
    content: @Composable LightingScopeReceiver.() -> Unit,
) {
    val state = remember { LightingState() }
    val receiver = remember(state) { LightingScopeReceiverImpl(state) }
    val shader = remember {
        Shader(buildGlobalBloomShader(MAX_LIGHTS)).getRuntimeShader(
            uniforms = basicUniformList.removeUniform("percentage").removeUniform("time"),
            customFunctions = emptySet(),
        )
    }
    var scopeOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                scopeOrigin = coords.positionInRoot()
            }
            .onSizeChanged { size ->
                state.sceneSize = size.toSize()
            }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val lights = state.uniforms // Invalidate only when lighting/geometry changes.
                renderEffect = if (lights.hasBloom) {
                    shader.setFloatUniform("resolution", size.width, size.height)
                    shader.setFloatUniform("scopeOrigin", scopeOrigin.x, scopeOrigin.y)
                    shader.setLightGeometry(lights)
                    shader.setFloatUniform("bloomIntensities", lights.bloomIntensities)
                    RenderEffect.createRuntimeShaderEffect(shader, "image").asComposeRenderEffect()
                } else {
                    null
                }
            }
    ) {
        receiver.content()
    }
}

// ── Internal implementation ───────────────────────────────────────────────────

private class LightingScopeReceiverImpl(
    private val state: LightingState,
) : LightingScopeReceiver {

    override fun Modifier.lightSource(
        color: Color,
        intensity: Float,
        bloomIntensity: Float,
    ): Modifier = composed {
        val key = remember { Any() }
        // null until the first layout pass — guards against registering at (0,0).
        var center by remember { mutableStateOf<Offset?>(null) }

        DisposableEffect(key) {
            onDispose { state.unregister(key) }
        }

        // Reading `center` HERE, in the composition phase, is what makes a layout-time
        // position update recompose this node. Without it the SideEffect below would only
        // re-run when some *other* input changed (e.g. animated intensity), so a light that
        // was already on when the screen opened would never register until a param changed.
        val resolvedCenter = center
        if (resolvedCenter != null) {
            // SideEffect runs after every recomposition — including every animation frame —
            // so position and intensity changes both propagate immediately.
            SideEffect {
                if (intensity > 0f || bloomIntensity > 0f) {
                    state.register(key, LightSource(resolvedCenter, color, intensity, bloomIntensity))
                } else {
                    state.unregister(key)
                }
            }
        }

        onGloballyPositioned { coords ->
            center = coords.boundsInRoot().center
        }
    }

    override fun Modifier.receivesLight(strength: Float, opaqueInset: Dp?): Modifier = composed {
        val shader = remember {
            Shader(buildElementReceiverShader(MAX_LIGHTS)).getRuntimeShader(
                uniforms = basicUniformList.removeUniform("percentage").removeUniform("time"),
                customFunctions = emptySet(),
            )
        }
        var origin by remember { mutableStateOf(Offset.Zero) }

        this
            .onGloballyPositioned { coords ->
                origin = coords.boundsInRoot().topLeft
            }
            .graphicsLayer {
                // Keep the original offscreen bounds (including shadow clipping) even
                // when the identity lighting pass is bypassed with every lamp off.
                compositingStrategy = CompositingStrategy.Offscreen
                val lights = state.uniforms
                val scene = state.sceneSize
                renderEffect = if (lights.hasLighting) {
                    val sceneScale = sqrt(scene.width * scene.width + scene.height * scene.height)
                    shader.setFloatUniform("resolution", size.width, size.height)
                    shader.setFloatUniform("elementOrigin", origin.x, origin.y)
                    shader.setFloatUniform("strength", strength)
                    shader.setFloatUniform("opaqueInset", opaqueInset?.toPx()?.coerceAtLeast(0f) ?: -1f)
                    shader.setFloatUniform("sceneScale", sceneScale.coerceAtLeast(1f))
                    shader.setLightGeometry(lights)
                    shader.setFloatUniform("lightIntensities", lights.intensities)
                    shader.setFloatUniform("glareColors", lights.glareColors)
                    RenderEffect.createRuntimeShaderEffect(shader, "image").asComposeRenderEffect()
                } else {
                    null
                }
            }
    }
}

// Light geometry (position + color) — used by both the bloom and receiver shaders.
private fun RuntimeShader.setLightGeometry(lights: LightUniforms) {
    setFloatUniform("lightPositions", lights.positions)
    setFloatUniform("lightColors", lights.colors)
}
