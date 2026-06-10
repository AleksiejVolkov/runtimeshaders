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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.offmind.runtimeshaders.composables.provideTimeAsState
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
            uniforms = basicUniformList.removeUniform("percentage")
        )
    }
    val time = provideTimeAsState()
    var scopeOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                scopeOrigin = coords.positionInRoot()
            }
            .onSizeChanged { size ->
                state.sceneSize = size.toSize()
                shader.setFloatUniform(
                    "resolution",
                    size.width.toFloat(),
                    size.height.toFloat(),
                )
            }
            .graphicsLayer {
                val lights = state.lights          // draw-phase read → layer invalidates on change
                shader.setFloatUniform("time", time.value)
                shader.setFloatUniform("scopeOrigin", scopeOrigin.x, scopeOrigin.y)
                shader.setLightGeometry(lights)
                shader.setBloomIntensities(lights)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
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

    override fun Modifier.receivesLight(strength: Float): Modifier = composed {
        val shader = remember {
            Shader(buildElementReceiverShader(MAX_LIGHTS)).getRuntimeShader(
                uniforms = basicUniformList.removeUniform("percentage")
            )
        }
        val time = provideTimeAsState()
        var origin by remember { mutableStateOf(Offset.Zero) }

        this
            .onGloballyPositioned { coords ->
                origin = coords.boundsInRoot().topLeft
            }
            .onSizeChanged { size ->
                shader.setFloatUniform(
                    "resolution",
                    size.width.toFloat(),
                    size.height.toFloat(),
                )
            }
            .graphicsLayer {
                val lights = state.lights
                val scene = state.sceneSize
                val sceneScale = sqrt(scene.width * scene.width + scene.height * scene.height)
                shader.setFloatUniform("time", time.value)
                shader.setFloatUniform("elementOrigin", origin.x, origin.y)
                shader.setFloatUniform("strength", strength)
                shader.setFloatUniform("sceneScale", sceneScale.coerceAtLeast(1f))
                shader.setLightGeometry(lights)
                shader.setLightIntensities(lights)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            }
    }
}

// Light geometry (position + color) — used by both the bloom and receiver shaders.
internal fun RuntimeShader.setLightGeometry(lights: Collection<LightSource>) {
    val positions = FloatArray(MAX_LIGHTS * 2)
    val colors    = FloatArray(MAX_LIGHTS * 3)

    lights.take(MAX_LIGHTS).forEachIndexed { i, light ->
        positions[i * 2]     = light.center.x
        positions[i * 2 + 1] = light.center.y
        colors[i * 3]        = light.color.red
        colors[i * 3 + 1]    = light.color.green
        colors[i * 3 + 2]    = light.color.blue
    }

    setFloatUniform("lightPositions", positions)
    setFloatUniform("lightColors",    colors)
}

// Receiver-specific: how strongly each light illuminates other elements.
internal fun RuntimeShader.setLightIntensities(lights: Collection<LightSource>) {
    val intensities = FloatArray(MAX_LIGHTS)
    lights.take(MAX_LIGHTS).forEachIndexed { i, light -> intensities[i] = light.intensity }
    setFloatUniform("lightIntensities", intensities)
}

// Bloom-specific: how strongly each light paints its own visible halo.
internal fun RuntimeShader.setBloomIntensities(lights: Collection<LightSource>) {
    val intensities = FloatArray(MAX_LIGHTS)
    lights.take(MAX_LIGHTS).forEachIndexed { i, light -> intensities[i] = light.bloomIntensity }
    setFloatUniform("bloomIntensities", intensities)
}
