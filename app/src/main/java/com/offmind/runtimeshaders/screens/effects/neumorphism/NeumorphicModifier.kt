package com.offmind.runtimeshaders.screens.effects.neumorphism

import android.graphics.RuntimeShader
import androidx.compose.runtime.compositionLocalOf
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

object NeuColors {
    val Base = Color(0xFFE0E5EC)    // the one material: background and every element
    val Dark = Color(0xFFA3B1C6)    // shadow, away from the light
    val Light = Color(0xFFFFFFFF)   // highlight, towards the light
    val Text = Color(0xFF4A5368)
    val Hint = Color(0xFF9AA4B5)
    val Accent = Color(0xFF3BD37A)  // status LED
}

// one light for the whole screen: a unit vector pointing towards it (screen space, y goes down)
val LocalLightDirection = compositionLocalOf { Offset(-0.7071f, -0.7071f) }

fun Modifier.neumorphic(
    cornerRadius: Dp,
    curve: Float = 0f,          // 0 = flat plate, < 0 = concave dish
    press: Float = 0f,          // 0 = raised, 1 = pressed into the surface
    distance: Dp = 8.dp,
    blur: Dp = 16.dp,
): Modifier = composed {
    val light = LocalLightDirection.current
    val shader = remember { RuntimeShader(neumorphicShader) }
    val brush = remember { ShaderBrush(shader) }
    drawBehind {
        // shadows live outside of the element: they can't go further than shift + blur
        val margin = (distance + blur).toPx() + 2f
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", min(cornerRadius.toPx(), size.minDimension / 2f))
        shader.setFloatUniform("lightDir", light.x, light.y)
        shader.setFloatUniform("shadowShift", distance.toPx())
        shader.setFloatUniform("shadowBlur", blur.toPx())
        shader.setFloatUniform("curve", curve)
        shader.setFloatUniform("press", press)
        shader.setColorUniform("baseColor", NeuColors.Base.toArgb())
        shader.setColorUniform("darkColor", NeuColors.Dark.toArgb())
        shader.setColorUniform("lightColor", NeuColors.Light.toArgb())
        drawRect(
            brush = brush,
            topLeft = Offset(-margin, -margin),
            size = Size(size.width + margin * 2, size.height + margin * 2)
        )
    }
}

@Language("AGSL")
private val neumorphicShader = """
    uniform float2 size;
    uniform float radius;
    uniform float2 lightDir;      // unit vector pointing towards the light
    uniform float shadowShift;    // how far the shadows are moved, px
    uniform float shadowBlur;     // how soft they are, px
    uniform float curve;          // 0 = flat, < 0 = concave
    uniform float press;          // 0 = raised, 1 = pressed in
    layout(color) uniform half4 baseColor;
    layout(color) uniform half4 darkColor;
    layout(color) uniform half4 lightColor;

    float sdRoundBox(float2 p, float2 halfSize, float r) {
        float2 q = abs(p) - halfSize + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    float Shape(float2 p) {
        return sdRoundBox(p - size * 0.5, size * 0.5, radius);
    }

    // a blurred copy of the shape straight from its sdf - no blur pass needed
    float Soft(float d, float blur) {
        return 1.0 - smoothstep(-blur, blur, d);
    }

    half4 main(float2 p) {
        float inside = 1.0 - smoothstep(-0.75, 0.75, Shape(p));

        // the same shape, moved away from the light and towards it
        float dark = Soft(Shape(p + lightDir * shadowShift), shadowBlur) * 0.62 * (1.0 - press);
        float lite = Soft(Shape(p - lightDir * shadowShift), shadowBlur) * 0.8 * (1.0 - press);

        // the highlight over the shadow, premultiplied
        half4 shadows = half4(lightColor.rgb * lite + darkColor.rgb * dark * (1.0 - lite),
                              lite + dark * (1.0 - lite));
        // the surface: Lambert on a very shallow dish
        float2 q = (p - size * 0.5) / (size * 0.5);          // -1..1 across the element
        float3 n = normalize(float3(2.0 * curve * q, 1.0));   // normal of the dish
        float3 L = normalize(float3(lightDir * 0.6, 0.8));    // the same light, a bit above the screen
        half4 surface = half4(baseColor.rgb * (1.0 + 0.9 * (dot(n, L) - L.z)), 1.0);

        // pressed: the same two shadows, but inside the shape and with the opposite sign
        float innerDark = 1.0 - Soft(Shape(p + lightDir * shadowShift * 0.5), shadowBlur * 0.5);
        float innerLite = 1.0 - Soft(Shape(p - lightDir * shadowShift * 0.5), shadowBlur * 0.5);
        surface.rgb = mix(surface.rgb, darkColor.rgb, innerDark * 0.6 * press);
        surface.rgb = mix(surface.rgb, lightColor.rgb, innerLite * 0.8 * press);
        return mix(shadows, surface, inside);
    }
""".trimIndent()
