package com.offmind.runtimeshaders.screens.effects.neumorphism

import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
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

// the same five colors for each theme; the shader doesn't care which one it gets
@Immutable
data class NeuPalette(
    val base: Color,
    val dark: Color,
    val light: Color,
    val text: Color,
    val hint: Color,
)

val LightPalette = NeuPalette(
    NeuColors.Base, NeuColors.Dark, NeuColors.Light, NeuColors.Text, NeuColors.Hint
)

// in the dark, the highlight can't be white: it's just a slightly lighter base
val DarkPalette = NeuPalette(
    base = Color(0xFF2B2F36),
    dark = Color(0xFF16181C),
    light = Color(0xFF3C414B),
    text = Color(0xFFD5DAE3),
    hint = Color(0xFF7D8696),
)

fun lerp(a: NeuPalette, b: NeuPalette, t: Float) = NeuPalette(
    base = lerp(a.base, b.base, t),
    dark = lerp(a.dark, b.dark, t),
    light = lerp(a.light, b.light, t),
    text = lerp(a.text, b.text, t),
    hint = lerp(a.hint, b.hint, t),
)

// a State again, so switching the theme only redraws the elements
val LocalNeuPalette = staticCompositionLocalOf<State<NeuPalette>> {
    mutableStateOf(LightPalette)
}

@Composable
fun neuColors(): NeuPalette = LocalNeuPalette.current.value

// one light for the whole screen: a unit vector pointing towards it (screen space, y goes down).
// It's a State, so a moving light only redraws the elements, without recomposition.
val DefaultLight = Offset(-0.7071f, -0.7071f)
val LocalLightDirection = staticCompositionLocalOf<State<Offset>> { mutableStateOf(DefaultLight) }

fun Modifier.neumorphic(
    cornerRadius: Dp,
    curve: Float = 0f,          // 0 = flat plate, < 0 = concave dish
    press: Float = 0f,          // 0 = raised, 1 = pressed into the surface
    distance: Dp = 8.dp,
    blur: Dp = 16.dp,
): Modifier = composed {
    val light = LocalLightDirection.current
    val palette = LocalNeuPalette.current
    val shader = remember { RuntimeShader(neumorphicShader) }
    val brush = remember { ShaderBrush(shader) }
    drawBehind {
        val l = light.value          // <-- read here, inside the draw phase
        val c = palette.value
        // shadows live outside of the element: they can't go further than shift + blur
        val margin = (distance + blur).toPx() + 2f
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", min(cornerRadius.toPx(), size.minDimension / 2f))
        shader.setFloatUniform("lightDir", l.x, l.y)
        shader.setFloatUniform("shadowShift", distance.toPx())
        shader.setFloatUniform("shadowBlur", blur.toPx())
        shader.setFloatUniform("curve", curve)
        shader.setFloatUniform("press", press)
        shader.setColorUniform("baseColor", c.base.toArgb())
        shader.setColorUniform("darkColor", c.dark.toArgb())
        shader.setColorUniform("lightColor", c.light.toArgb())
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
