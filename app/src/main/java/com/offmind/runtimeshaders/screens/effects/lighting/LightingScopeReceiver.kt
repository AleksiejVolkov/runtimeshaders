package com.offmind.runtimeshaders.screens.effects.lighting

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Receiver scope provided by [LightingScope], mirroring the pattern of [androidx.compose.foundation.layout.RowScope].
 *
 * Composables declared as extensions on [LightingScopeReceiver] gain access to
 * [lightSource], [receivesLight] and [excludeLight] as plain modifier calls — no
 * CompositionLocal needed.
 *
 * ```kotlin
 * LightingScope {
 *     MyCard(modifier = Modifier.lightSource(...))   // 'this' is LightingScopeReceiver
 * }
 *
 * @Composable
 * fun LightingScopeReceiver.MyCard(modifier: Modifier = Modifier) { … }
 * ```
 */
interface LightingScopeReceiver {

    /**
     * Registers this element as a point light source.
     * [intensity] is the primary on/off control — animate it with [animateFloatAsState]
     * for a smooth fade. 0f = light off, 1f = full brightness.
     */
    fun Modifier.lightSource(
        color: Color,
        radius: Dp,
        bloomRadius: Dp = radius,
        intensity: Float = 1f,
    ): Modifier

    /**
     * Applies a per-element lighting shader that brightens this element's own pixels
     * based on distance to every active light source. [strength] scales the response.
     */
    fun Modifier.receivesLight(strength: Float = 1f): Modifier

    /**
     * Masks the global bloom out of this element's bounds. Pair it with a co-located
     * [lightSource] so the glow appears to spill from under/around the element rather
     * than washing over its own pixels — like light leaking out from beneath a fixture.
     *
     * [feather] softens the mask edge: a larger value lets more glow bleed inward.
     */
    fun Modifier.excludeLight(feather: Dp = 12.dp): Modifier
}
