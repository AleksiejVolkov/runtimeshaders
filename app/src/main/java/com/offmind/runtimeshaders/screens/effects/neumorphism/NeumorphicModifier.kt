package com.offmind.runtimeshaders.screens.effects.neumorphism

import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import org.intellij.lang.annotations.Language
import kotlin.math.min

object NeuColors {
    val Base = Color(0xFFE0E5EC)    // the one material: background and every element
    val Dark = Color(0xFFA3B1C6)    // shadow, away from the light
    val Light = Color(0xFFFFFFFF)   // highlight, towards the light
    val Text = Color(0xFF4A5368)
    val Hint = Color(0xFF9AA4B5)
}

fun Modifier.neumorphic(
    cornerRadius: Dp,
): Modifier = composed {
    val shader = remember { RuntimeShader(neumorphicShader) }
    val brush = remember { ShaderBrush(shader) }
    drawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", min(cornerRadius.toPx(), size.minDimension / 2f))
        drawRect(brush)
    }
}

@Language("AGSL")
private val neumorphicShader = """
    uniform float2 size;
    uniform float radius;

    float sdRoundBox(float2 p, float2 halfSize, float r) {
        float2 q = abs(p) - halfSize + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    float Shape(float2 p) {
        return sdRoundBox(p - size * 0.5, size * 0.5, radius);
    }

    half4 main(float2 p) {
        float inside = 1.0 - smoothstep(-0.75, 0.75, Shape(p));
        return half4(1.0, 0.3, 0.3, 1.0) * inside;   // debug: paint the shape red for a moment
    }
""".trimIndent()
