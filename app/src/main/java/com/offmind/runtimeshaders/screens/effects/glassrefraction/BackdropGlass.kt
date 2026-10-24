package com.offmind.runtimeshaders.screens.effects.glassrefraction

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
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

// The glass is drawn by the backdrop, not by the panel: this layer already has every pixel
// the glass needs to bend.
fun Modifier.glassBackdrop(panel: GlassPanel): Modifier = composed {
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

            val backdrop = RenderEffect.createRuntimeShaderEffect(identity, "image")
            val cut = RenderEffect.createRuntimeShaderEffect(glass, "image")
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

// Sees the whole backdrop, but keeps only what is under the panel.
@Language("AGSL")
private val glassShader = """
    uniform shader image;
    uniform float4 panel;          // left, top, width, height in backdrop pixels
    uniform float cornerRadius;

    float sdRoundedBox(float2 p, float2 halfSize, float radius) {
        float2 q = abs(p) - halfSize + radius;
        return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;
    }

    half4 main(float2 xy) {
        float2 halfSize = panel.zw * 0.5;
        float sd = sdRoundedBox(xy - panel.xy - halfSize, halfSize, cornerRadius);
        // 1 inside, 0 outside, one smooth pixel on the edge
        float coverage = clamp(0.5 - sd, 0.0, 1.0);
        if (coverage == 0.0) return half4(0.0);

        half3 color = image.eval(xy).rgb;
        color = mix(color, half3(1.0), 0.25);   // milky for now, to see the cut
        return half4(color, 1.0) * coverage;
    }
"""
