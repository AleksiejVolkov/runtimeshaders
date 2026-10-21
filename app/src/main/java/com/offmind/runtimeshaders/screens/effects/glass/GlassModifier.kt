package com.offmind.runtimeshaders.screens.effects.glass

import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language
import kotlin.math.min

// one light for every piece of glass: top left, a bit more from above
val GlassLight = Offset(-0.6f, -0.8f)

fun Modifier.glass(
    cornerRadius: Dp,
    tint: Color = Color.White,
    tintAlpha: Float = 0.10f,   // how much of the body we see: glass is mostly transparent
    edge: Dp = 2.5.dp,          // the rounded edge of the glass: thin, this is where light is caught
    shine: Color = Color.White, // the color of the light reflected by the edges
    light: Offset = GlassLight, // unit vector towards the light, screen space (y goes down)
): Modifier = composed {
    val shader = remember { RuntimeShader(glassShader) }
    val brush = remember { ShaderBrush(shader) }
    drawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", min(cornerRadius.toPx(), size.minDimension / 2f))
        shader.setColorUniform("tint", tint.toArgb())
        shader.setFloatUniform("tintAlpha", tintAlpha)
        shader.setFloatUniform("edge", edge.toPx())
        shader.setColorUniform("shine", shine.toArgb())
        shader.setFloatUniform("lightDir", light.x, light.y)
        drawRect(brush)
    }
}

@Language("AGSL")
private val glassShader = """
    uniform float2 size;
    uniform float radius;
    layout(color) uniform half4 tint;
    uniform float tintAlpha;
    uniform float edge;               // px, width of the rounded edge
    layout(color) uniform half4 shine;
    uniform float2 lightDir;

    float sdRoundBox(float2 p, float2 halfSize, float r) {
        float2 q = abs(p) - halfSize + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    float Shape(float2 p) {
        return sdRoundBox(p - size * 0.5, size * 0.5, radius);
    }

    // outward direction of the edge: the gradient of the distance
    float2 EdgeDir(float2 p) {
        float2 g = float2(Shape(p + float2(1.0, 0.0)) - Shape(p - float2(1.0, 0.0)),
                          Shape(p + float2(0.0, 1.0)) - Shape(p - float2(0.0, 1.0)));
        return normalize(g + 1e-4);
    }

    // light laid over the glass: a premultiplied "over", with its own alpha.
    // (rgb can't be bigger than alpha in a premultiplied color, so light needs alpha too)
    half4 Over(half4 dst, half3 color, float k) {
        k = clamp(k, 0.0, 1.0);
        return half4(color * k + dst.rgb * (1.0 - k), k + dst.a * (1.0 - k));
    }

    half4 main(float2 p) {
        float d = Shape(p);
        float inside = 1.0 - smoothstep(-0.75, 0.75, d);

        // the edge of a glass plate is rounded, but it's thin: a couple of dp
        float e = clamp(-d / edge, 0.0, 1.0);         // 0 on the edge, 1 where the flat top begins
        float s = 1.0 - e;                            // how steep the surface is here
        float3 n = normalize(float3(EdgeDir(p) * s, sqrt(max(1.0 - s * s, 0.0))));

        // Fresnel (after Schlick): the flatter we look at glass, the more of a mirror it becomes.
        // On a flat plate that only happens on the edge, so this is a thin line all around.
        // (Schlick uses the power of 5; on a 2dp edge that's thinner than a pixel, so I use 2)
        float fresnel = pow(1.0 - n.z, 2.0);

        // which way this piece of the edge looks: towards the light, or away from it.
        float facing = dot(EdgeDir(p), lightDir);

        // the edge facing the light mirrors it: that's the key highlight. The opposite edge
        // shines too, weaker: light went through the plate and comes out there
        float key = pow(max(facing, 0.0), 1.5);
        float back = pow(max(-facing, 0.0), 1.5);
        float rim = fresnel * (0.45 + 1.4 * key + 0.9 * back);

        // the flat top is a weak mirror too (Fresnel never goes to zero): it reflects a bright sky
        // on the side of the light and fades to nothing on the other side
        float along = dot((p - size * 0.5) / max(size.x, size.y), -lightDir);   // -0.5..0.5
        float sky = smoothstep(0.1, -0.5, along) * 0.07;

        // the body: a thin tinted filter over whatever is behind it (premultiplied)
        float a = tintAlpha * inside;
        half4 glass = half4(tint.rgb * a, a);
        // the reflected light lies on top of it
        glass = Over(glass, shine.rgb, (sky + rim) * inside);
        return glass;
    }
""".trimIndent()
