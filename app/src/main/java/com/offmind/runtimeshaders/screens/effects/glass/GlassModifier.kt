package com.offmind.runtimeshaders.screens.effects.glass

import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import org.intellij.lang.annotations.Language
import kotlin.math.min

fun Modifier.glass(
    cornerRadius: Dp,
    tint: Color = Color.White,
    tintAlpha: Float = 0.10f,   // how much of the body we see: glass is mostly transparent
): Modifier = composed {
    val shader = remember { RuntimeShader(glassShader) }
    val brush = remember { ShaderBrush(shader) }
    drawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", min(cornerRadius.toPx(), size.minDimension / 2f))
        shader.setColorUniform("tint", tint.toArgb())
        shader.setFloatUniform("tintAlpha", tintAlpha)
        drawRect(brush)
    }
}

@Language("AGSL")
private val glassShader = """
    uniform float2 size;
    uniform float radius;
    layout(color) uniform half4 tint;
    uniform float tintAlpha;

    float sdRoundBox(float2 p, float2 halfSize, float r) {
        float2 q = abs(p) - halfSize + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    float Shape(float2 p) {
        return sdRoundBox(p - size * 0.5, size * 0.5, radius);
    }

    half4 main(float2 p) {
        float d = Shape(p);
        float inside = 1.0 - smoothstep(-0.75, 0.75, d);

        // the body: a thin tinted filter over whatever is behind it (premultiplied)
        float a = tintAlpha * inside;
        return half4(tint.rgb * a, a);
    }
""".trimIndent()
