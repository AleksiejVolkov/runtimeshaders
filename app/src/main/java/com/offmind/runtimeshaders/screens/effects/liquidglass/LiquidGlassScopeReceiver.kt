package com.offmind.runtimeshaders.screens.effects.liquidglass

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Receiver scope provided by [LiquidGlassScope], mirroring the pattern of
 * [com.offmind.runtimeshaders.screens.effects.lighting.LightingScopeReceiver].
 *
 * ```kotlin
 * LiquidGlassScope {
 *     LazyColumn(Modifier.glassBackdrop()) { … }            // what the glass can "see"
 *     TopBar(Modifier.liquidGlass(cornerRadius = 32.dp))    // blurs/refracts the backdrop
 * }
 * ```
 *
 * Skia render effects only ever receive the layer's *own* content, so glass elements cannot
 * sample arbitrary siblings. Instead the backdrop subtree is recorded into a shared
 * [androidx.compose.ui.graphics.layer.GraphicsLayer] (a display list — no bitmap copies),
 * and each glass element replays the region behind itself with a blur + liquid-glass shader.
 */
interface LiquidGlassScopeReceiver {

    /**
     * Marks this subtree as the content glass elements sample from. The content is recorded
     * into the scope's shared graphics layer and still draws to screen normally.
     *
     * Glass elements must be *siblings* of (not children inside) the backdrop subtree,
     * otherwise they would sample themselves.
     */
    fun Modifier.glassBackdrop(): Modifier

    /**
     * Turns this element into a liquid-glass surface: the backdrop region behind it is
     * replayed, blurred by [blurRadius], then run through an AGSL shader that refracts a
     * [refractionHeight]-wide band along the rounded-rect rim by up to [refractionAmount],
     * boosts saturation by [vibrancy], washes the material with [tint], and paints
     * specular rim highlights. The element is clipped to a rounded rect of [cornerRadius].
     */
    fun Modifier.liquidGlass(
        cornerRadius: Dp,
        blurRadius: Dp = 12.dp,
        refractionHeight: Dp = 14.dp,
        refractionAmount: Dp = 12.dp,
        tint: Color = Color.White.copy(alpha = 0.08f),
        vibrancy: Float = 0.15f,
    ): Modifier
}
