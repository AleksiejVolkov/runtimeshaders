package com.offmind.runtimeshaders.screens.effects.neumorphism

import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language

// A widget track is a groove carved into the material: a ring (or a part of it) or a capsule
enum class TrackShape { Ring, Bar }

fun Modifier.accentTrack(
    shape: TrackShape,
    progress: Float,          // how much of the track is filled, 0..1
    colors: Pair<Color, Color>,   // accent at the start and at the end of the track
    thickness: Dp = 18.dp,
    startDeg: Float = 0f,     // ring only: where the track starts, 0 = top, clockwise
    sweepDeg: Float = 360f,   // ring only: how much of the circle the track takes
): Modifier = composed {
    val light = LocalLightDirection.current
    val palette = LocalNeuPalette.current
    val shader = remember { RuntimeShader(accentShader) }
    val brush = remember { ShaderBrush(shader) }
    drawBehind {
        val l = light.value
        val c = palette.value
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("kind", if (shape == TrackShape.Ring) 0f else 1f)
        shader.setFloatUniform("thickness", thickness.toPx())
        shader.setFloatUniform("startAngle", Math.toRadians(startDeg.toDouble()).toFloat())
        shader.setFloatUniform("sweep", Math.toRadians(sweepDeg.toDouble()).toFloat())
        shader.setFloatUniform("progress", progress.coerceIn(0f, 1f))
        shader.setColorUniform("accentA", colors.first.toArgb())
        shader.setColorUniform("accentB", colors.second.toArgb())
        shader.setFloatUniform("lightDir", l.x, l.y)
        shader.setFloatUniform("shadowShift", 3.dp.toPx())
        shader.setFloatUniform("shadowBlur", 5.dp.toPx())
        shader.setColorUniform("baseColor", c.base.toArgb())
        shader.setColorUniform("darkColor", c.dark.toArgb())
        shader.setColorUniform("lightColor", c.light.toArgb())
        drawRect(brush)
    }
}

@Language("AGSL")
private val accentShader = """
    uniform float2 size;
    uniform float kind;           // 0 = ring, 1 = bar
    uniform float thickness;      // width of the groove, px
    uniform float startAngle;     // ring: where the track starts, radians, 0 = top
    uniform float sweep;          // ring: how much of the circle it takes, radians
    uniform float progress;       // filled part of the track, 0..1
    layout(color) uniform half4 accentA;
    layout(color) uniform half4 accentB;
    uniform float2 lightDir;
    uniform float shadowShift;
    uniform float shadowBlur;
    layout(color) uniform half4 baseColor;
    layout(color) uniform half4 darkColor;
    layout(color) uniform half4 lightColor;

    const float TAU = 6.2831853;

    // angle of a point around the centre: 0 at the top, growing clockwise
    float Angle(float2 q) {
        float a = atan(q.x, -q.y);
        return a < 0.0 ? a + TAU : a;
    }

    // a ring of radius R and half-width r, cut to [start, start + len]
    float ArcSdf(float2 q, float R, float r, float start, float len) {
        float u = mod(Angle(q) - start + TAU, TAU);
        if (u <= len) return abs(length(q) - R) - r;
        float2 p0 = R * float2(sin(start), -cos(start));
        float2 p1 = R * float2(sin(start + len), -cos(start + len));
        return min(length(q - p0), length(q - p1)) - r;
    }

    float SegmentSdf(float2 q, float2 a, float2 b) {
        float2 pa = q - a, ba = b - a;
        float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
        return length(pa - ba * h);
    }

    // ends of the bar: left to right, or bottom to top for a tall one
    float2 BarStart() {
        return size.x >= size.y ? float2(-size.x * 0.5 + thickness * 0.5, 0.0)
                                : float2(0.0, size.y * 0.5 - thickness * 0.5);
    }
    float2 BarEnd() { return -BarStart(); }

    float Radius() { return min(size.x, size.y) * 0.5 - thickness * 0.5; }

    float Track(float2 p) {
        float2 q = p - size * 0.5;
        float r = thickness * 0.5;
        if (kind < 0.5) return ArcSdf(q, Radius(), r, startAngle, sweep);
        return SegmentSdf(q, BarStart(), BarEnd()) - r;
    }

    // the fill is the same shape, shorter and a bit thinner: it lies at the bottom of the groove
    float Fill(float2 p) {
        float2 q = p - size * 0.5;
        float r = thickness * 0.3;
        if (kind < 0.5) return ArcSdf(q, Radius(), r, startAngle, sweep * progress);
        float2 a = BarStart();
        return SegmentSdf(q, a, mix(a, BarEnd(), progress)) - r;
    }

    // where along the track a point is, 0..1: the color depends on it, not on x or y
    float Along(float2 p) {
        float2 q = p - size * 0.5;
        if (kind < 0.5) {
            float len = sweep * progress;
            float u = mod(Angle(q) - startAngle + TAU, TAU);
            if (u > len) {
                // in the round caps: take the closest end of the fill
                float2 p0 = Radius() * float2(sin(startAngle), -cos(startAngle));
                float2 p1 = Radius() * float2(sin(startAngle + len), -cos(startAngle + len));
                u = length(q - p0) < length(q - p1) ? 0.0 : len;
            }
            return u / sweep;
        }
        float2 a = BarStart(), b = BarEnd();
        return clamp(dot(q - a, b - a) / dot(b - a, b - a), 0.0, 1.0);
    }

    float Soft(float d, float blur) {
        return 1.0 - smoothstep(-blur, blur, d);
    }

    half4 main(float2 p) {
        float d = Track(p);
        float inside = 1.0 - smoothstep(-0.75, 0.75, d);

        // a groove is just a pressed shape: the same two soft copies, inverted
        float innerDark = 1.0 - Soft(Track(p + lightDir * shadowShift), shadowBlur);
        float innerLite = 1.0 - Soft(Track(p - lightDir * shadowShift), shadowBlur);
        half3 col = baseColor.rgb;
        col = mix(col, darkColor.rgb, innerDark * 0.75);
        col = mix(col, lightColor.rgb, innerLite * 0.8);

        // the accent: the gradient follows the track
        float fill = 1.0 - smoothstep(-0.75, 0.75, Fill(p));
        half3 accent = mix(accentA.rgb, accentB.rgb, Along(p));
        col = mix(col, accent, fill);

        return half4(col, 1.0) * inside;
    }
""".trimIndent()
