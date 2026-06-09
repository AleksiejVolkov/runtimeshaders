package com.offmind.runtimeshaders.screens.effects.lighting

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Receiver scope provided by [LightingScope], mirroring the pattern of [androidx.compose.foundation.layout.RowScope].
 *
 * Composables declared as extensions on [LightingScopeReceiver] gain access to
 * [lightSource] and [receivesLight] as plain modifier calls — no CompositionLocal needed.
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
     * Registers this element as a point light source. Light falls off across the whole
     * scene with a fixed exponential profile; the two knobs scale that same profile:
     *
     * - [intensity] — how strongly this light tints *other* elements ([receivesLight]).
     * - [bloomIntensity] — how strongly the light paints its *own* visible halo onto the
     *   canvas. 0f draws no bloom, 1f matches the lit field, 2f is twice as bright.
     *
     * Animate either with [animateFloatAsState] for a smooth fade (0f = off).
     */
    fun Modifier.lightSource(
        color: Color,
        intensity: Float = 1f,
        bloomIntensity: Float = 1f,
    ): Modifier

    /**
     * Applies a per-element lighting shader that brightens this element's own pixels
     * based on distance to every active light source. [strength] scales the response.
     */
    fun Modifier.receivesLight(strength: Float = 1f): Modifier
}
