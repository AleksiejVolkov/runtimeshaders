package com.offmind.runtimeshaders.screens.effects.glassrefraction

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.intellij.lang.annotations.Language

// Where the glass is. The panel writes it, the backdrop reads it.
class GlassPanel {
    var bounds by mutableStateOf(Rect.Zero) // in root coordinates
    var cornerRadius by mutableStateOf(0.dp)
}

// The panel itself draws nothing: it only reports where it is.
fun Modifier.glassPanel(panel: GlassPanel, cornerRadius: Dp): Modifier = onGloballyPositioned {
    panel.bounds = Rect(it.positionInRoot(), it.size.toSize())
    panel.cornerRadius = cornerRadius
}

// A thick piece of glass: flat on top, rounded down to the backdrop near its edge
private val Thickness = 70.dp
private val Bevel = 100.dp
private const val IndexOfRefraction = 1.5f // glass
private val FrostRadius = 16.dp

// The glass is drawn by the backdrop, not by the panel: this layer already has every pixel
// the glass needs to bend.
fun Modifier.glassBackdrop(panel: GlassPanel, frosted: Boolean = false): Modifier = composed {
    val identity = remember { RuntimeShader(identityShader) }
    val glass = remember { RuntimeShader(glassShader) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .graphicsLayer {
            // the panel in the backdrop's own pixels
            val r = panel.bounds.translate(-origin)
            glass.setFloatUniform("panel", r.left, r.top, r.width, r.height)
            glass.setFloatUniform("cornerRadius", panel.cornerRadius.toPx())
            glass.setFloatUniform("thickness", Thickness.toPx())
            glass.setFloatUniform("bevel", Bevel.toPx())
            glass.setFloatUniform("ior", IndexOfRefraction)

            val backdrop = RenderEffect.createRuntimeShaderEffect(identity, "image")
            var cut = RenderEffect.createRuntimeShaderEffect(glass, "image")
            if (frosted) {
                // the glass now bends an already blurred backdrop
                val radius = FrostRadius.toPx()
                val blur = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                cut = RenderEffect.createChainEffect(cut, blur)
            }
            // the glass, laid over the untouched backdrop
            renderEffect = RenderEffect
                .createBlendModeEffect(backdrop, cut, BlendMode.SRC_OVER)
                .asComposeRenderEffect()
        }
}

// Changes nothing: the backdrop goes through a shader and comes out as it was.
@Language("AGSL")
private val identityShader = """
    uniform shader image;

    half4 main(float2 xy) {
        return image.eval(xy);
    }
"""

// Sees the whole backdrop, but keeps only what is under the panel, and bends it the way a
// thick piece of glass would: Snell's law at the top surface, then straight down to the
// backdrop.
@Language("AGSL")
private val glassShader = """
    uniform shader image;
    uniform float4 panel;          // left, top, width, height in backdrop pixels
    uniform float cornerRadius;
    uniform float thickness;       // px
    uniform float bevel;           // px, how far in from the edge the glass is rounded
    uniform float ior;             // index of refraction

    float sdRoundedBox(float2 p, float2 halfSize, float radius) {
        float2 q = abs(p) - halfSize + radius;
        return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;
    }

    // which way is out: the gradient of the distance field
    float2 outwardAt(float2 p, float2 halfSize, float radius) {
        float2 dx = float2(1.0, 0.0);
        float2 dy = float2(0.0, 1.0);
        return normalize(float2(
            sdRoundedBox(p + dx, halfSize, radius) - sdRoundedBox(p - dx, halfSize, radius),
            sdRoundedBox(p + dy, halfSize, radius) - sdRoundedBox(p - dy, halfSize, radius)
        ) + 1e-5);
    }

    // The profile of the glass at distance d in from its edge: a quarter circle over the
    // bevel, flat after it. x is 1 at the edge and 0 where the flat top begins.
    float glassHeight(float x) {
        return thickness * sqrt(1.0 - x * x);
    }

    // How fast the height grows going inward, dh/dd.
    float glassSlope(float x, float b) {
        return thickness / b * x / sqrt(max(1.0 - x * x, 1e-3));
    }

    half4 main(float2 xy) {
        float2 halfSize = panel.zw * 0.5;
        float2 p = xy - panel.xy - halfSize;
        float sd = sdRoundedBox(p, halfSize, cornerRadius);
        // 1 inside, 0 outside, one smooth pixel on the edge
        float coverage = clamp(0.5 - sd, 0.0, 1.0);
        if (coverage == 0.0) return half4(0.0);

        // The glass surface follows the same outline, but with corners at least as round as
        // the bevel: inside a rectangle the nearest edge flips on the diagonal, and with
        // rounder corners that flip happens on the flat top, where it bends nothing.
        float b = min(bevel, min(halfSize.x, halfSize.y));
        float glassRadius = max(cornerRadius, b);
        float sdGlass = sdRoundedBox(p, halfSize, glassRadius);
        float2 outward = outwardAt(p, halfSize, glassRadius);
        float x = clamp(1.0 + sdGlass / b, 0.0, 1.0);

        // the surface rises inward, so on the bevel it leans outward
        float3 normal = normalize(float3(outward * glassSlope(x, b), 1.0));
        // we look straight down at the screen; the ray enters the glass and bends
        float3 ray = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / ior);
        // ... and goes on through the glass down to the backdrop
        float2 shift = ray.xy * glassHeight(x) / -ray.z;

        half3 color = image.eval(xy + shift).rgb;
        color = mix(color, half3(1.0), 0.06);
        return half4(color, 1.0) * coverage;
    }
"""
