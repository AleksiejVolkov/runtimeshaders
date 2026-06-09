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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.removeUniform

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
        Shader(buildGlobalBloomShader(MAX_LIGHTS, MAX_EXCLUSIONS)).getRuntimeShader(
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
                shader.setFloatUniform(
                    "resolution",
                    size.width.toFloat(),
                    size.height.toFloat(),
                )
            }
            .graphicsLayer {
                val lights = state.lights          // draw-phase read → layer invalidates on change
                val exclusions = state.exclusions
                shader.setFloatUniform("time", time.value)
                shader.setFloatUniform("scopeOrigin", scopeOrigin.x, scopeOrigin.y)
                shader.setSharedLightUniforms(lights)
                shader.setBloomRadii(lights)
                shader.setExclusionUniforms(exclusions)
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
        radius: Dp,
        bloomRadius: Dp,
        intensity: Float,
    ): Modifier = composed {
        val density = LocalDensity.current
        val radiusPx      = with(density) { radius.toPx() }
        val bloomRadiusPx = with(density) { bloomRadius.toPx() }
        val key = remember { Any() }
        var center by remember { mutableStateOf(Offset.Zero) }
        // Guard against registering at (0,0) before the first layout pass.
        var hasPosition by remember { mutableStateOf(false) }

        DisposableEffect(key) {
            onDispose { state.unregister(key) }
        }

        // SideEffect runs after every recomposition — including every animation frame —
        // so intensity changes driven by animateFloatAsState propagate immediately.
        SideEffect {
            if (hasPosition) {
                if (intensity > 0f) {
                    state.register(key, LightSource(center, color, radiusPx, bloomRadiusPx, intensity))
                } else {
                    state.unregister(key)
                }
            }
        }

        onGloballyPositioned { coords ->
            center = coords.boundsInRoot().center
            hasPosition = true
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
                shader.setFloatUniform("time", time.value)
                shader.setFloatUniform("elementOrigin", origin.x, origin.y)
                shader.setFloatUniform("strength", strength)
                shader.setSharedLightUniforms(lights)
                shader.setReceiverRadii(lights)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            }
    }

    override fun Modifier.excludeLight(feather: Dp): Modifier = composed {
        val featherPx = with(LocalDensity.current) { feather.toPx() }
        val key = remember { Any() }

        DisposableEffect(key) {
            onDispose { state.unregisterExclusion(key) }
        }

        onGloballyPositioned { coords ->
            state.registerExclusion(key, ExclusionZone(coords.boundsInRoot(), featherPx))
        }
    }
}

// Shared uniforms used by both the bloom and receiver shaders.
internal fun RuntimeShader.setSharedLightUniforms(lights: Collection<LightSource>) {
    val positions   = FloatArray(MAX_LIGHTS * 2)
    val colors      = FloatArray(MAX_LIGHTS * 3)
    val intensities = FloatArray(MAX_LIGHTS)

    lights.take(MAX_LIGHTS).forEachIndexed { i, light ->
        positions[i * 2]     = light.center.x
        positions[i * 2 + 1] = light.center.y
        colors[i * 3]        = light.color.red
        colors[i * 3 + 1]    = light.color.green
        colors[i * 3 + 2]    = light.color.blue
        intensities[i]       = light.intensity
    }

    setFloatUniform("lightPositions",   positions)
    setFloatUniform("lightColors",      colors)
    setFloatUniform("lightIntensities", intensities)
}

// Bloom-specific: uses bloomRadiusPx so halo size is independent of receiver reach.
internal fun RuntimeShader.setBloomRadii(lights: Collection<LightSource>) {
    val radii = FloatArray(MAX_LIGHTS)
    lights.take(MAX_LIGHTS).forEachIndexed { i, light -> radii[i] = light.bloomRadiusPx }
    setFloatUniform("bloomRadii", radii)
}

// Receiver-specific: uses radiusPx so receiver sensitivity is independent of halo size.
internal fun RuntimeShader.setReceiverRadii(lights: Collection<LightSource>) {
    val radii = FloatArray(MAX_LIGHTS)
    lights.take(MAX_LIGHTS).forEachIndexed { i, light -> radii[i] = light.radiusPx }
    setFloatUniform("lightRadii", radii)
}

// Bloom-specific: rectangles (root space) where the bloom is carved away.
internal fun RuntimeShader.setExclusionUniforms(exclusions: Collection<ExclusionZone>) {
    val rects   = FloatArray(MAX_EXCLUSIONS * 4)
    val feather = FloatArray(MAX_EXCLUSIONS)

    exclusions.take(MAX_EXCLUSIONS).forEachIndexed { i, zone ->
        rects[i * 4]     = zone.bounds.left
        rects[i * 4 + 1] = zone.bounds.top
        rects[i * 4 + 2] = zone.bounds.right
        rects[i * 4 + 3] = zone.bounds.bottom
        feather[i]       = zone.featherPx
    }

    setFloatUniform("exclusionRects", rects)
    setFloatUniform("exclusionFeather", feather)
}
