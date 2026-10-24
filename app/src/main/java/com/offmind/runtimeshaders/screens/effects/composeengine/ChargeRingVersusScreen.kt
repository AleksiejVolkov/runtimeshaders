package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language
import kotlin.math.sin

// The scene, as fractions of the box: the ring sits a little above the centre, the stream rises
// from the bottom edge into the ring's lowest point.
private const val RING_R = 0.27f
private const val RING_Y = 0.44f
private val Blue = Color(0xFF3D6BFF)
private val Violet = Color(0xFF8A4DFF)
private val Cyan = Color(0xFF8FD8FF)

@Language("AGSL")
private const val CHARGE_SHADER = """
uniform float2 size;
uniform float time;
uniform float level;      // charge, 0 … 1

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), u.x), mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
}

half4 main(float2 p) {
    float s = size.x;
    float2 c = float2(0.5 * s, 0.44 * size.y);
    float R = 0.27 * s;
    float2 d = p - c;
    float r = length(d);
    float a = atan(d.y, d.x);
    float2 dir = d / max(r, 0.001);

    // The ring: lit clockwise from the bottom up to the charge level, its width breathing.
    float around = fract((a - 1.5707963) / 6.2831853 + 1.0);
    float lit = 0.22 + 0.78 * smoothstep(level + 0.01, level - 0.01, around);
    float width = s * (0.006 + 0.004 * noise(dir * 3.0 + time * 0.7));
    float ringD = abs(r - R);
    float ring = exp(-ringD * ringD / (2.0 * width * width)) * lit;
    float halo = exp(-ringD / (s * 0.06)) * 0.35 * lit;

    // Rays: flares around the ring whose length and flicker come from noise along the angle,
    // seamless because it is sampled on a circle; stronger as the charge grows.
    float n = noise(dir * 2.6 + float2(time * 0.35, -time * 0.21));
    float flare = n * n * (0.35 + 0.9 * level);
    float along = (r - R) / (s * (0.06 + 0.22 * flare));
    float streak = 0.55 + 0.45 * noise(float2(a * 38.0, r * 0.012 - time * 1.6));
    float rays = r > R ? exp(-along * 2.2) * flare * streak : 0.0;

    // The stream: rising from the bottom into the ring, wavering, with sparks running up it.
    float below = smoothstep(c.y + R * 0.92, c.y + R * 1.05, p.y);
    float sx = p.x - c.x + sin(p.y * 0.021 - time * 3.1) * s * 0.004;
    float core = exp(-sx * sx / (2.0 * pow(s * (0.004 + 0.004 * level), 2.0)));
    float glow = exp(-abs(sx) / (s * 0.03)) * 0.35;
    float flow = 0.6 + 0.4 * noise(float2(sx * 0.15, p.y * 0.02 + time * 5.0));
    float sparks = step(0.985, noise(float2(sx * 0.6, p.y * 0.08 + time * 9.0))) * exp(-abs(sx) / (s * 0.02));
    float stream = below * ((core + glow) * flow * (0.5 + level) + sparks);

    half3 col = half3(0.24, 0.42, 1.0) * half(ring * 1.6 + halo)
              + half3(0.54, 0.30, 1.0) * half(rays)
              + half3(0.56, 0.85, 1.0) * half(stream)
              + half3(0.9, 0.95, 1.0) * half(ring * ring * 0.6);
    col = half3(1.0) - exp(-col * 1.4);   // soft tone curve: bright parts bloom to white
    return half4(col, 1.0);
}
"""

private enum class ChargeImpl(val label: String) {
    Shader("AGSL"),
    NoShader("Without a shader: gradients + blur"),
}

@Composable
internal fun ChargeRingVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(ChargeImpl.Shader) }
    val transition = rememberInfiniteTransition(label = "charge")
    val time = transition.animateFloat(0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing)), label = "time")
    val level = transition.animateFloat(0.05f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "level")
    val percent by remember { derivedStateOf { (level.value * 100).toInt() } }
    val frames by rememberFrameSummary("screen=vs-charge variant=${impl.name}")
    val alloc = recordAllocations("vs-charge", impl.name)

    EngineScreen(
        title = "A live charging ring",
        subtitle = "Flickering rays and a flowing stream that grow with the charge.",
    ) {
        EngineChoice("Implementation", ChargeImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        Box(Modifier.fillMaxWidth().aspectRatio(0.8f).background(Color.Black)) {
            when (impl) {
                ChargeImpl.Shader -> ShaderCharge(time, level, Modifier.fillMaxSize())
                ChargeImpl.NoShader -> GradientCharge(time, level, Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxWidth().aspectRatio(0.8f / (RING_Y * 2f))) {
                Text(
                    "$percent%",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        Text(
            if (impl == ChargeImpl.Shader) "one pass: ring, rays and stream from noise over angle and time"
            else "drawArc + 48 ray wedges (sums of sines) + wavy stream lines + sparks, in one blurred glow layer",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShaderCharge(time: State<Float>, level: State<Float>, modifier: Modifier) {
    val shader = remember { newRuntimeShader(CHARGE_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier.drawBehind {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("time", time.value)
            shader.setFloatUniform("level", level.value)
            drawRect(brush)
        },
    )
}

private const val RAYS = 48

/**
 * The best attempt without a custom shader. Everything that glows is drawn twice: once into a
 * layer blurred with RenderEffect (the bloom), once sharp on top. Ray lengths flicker as sums of
 * two sines with fixed per-ray phases; the stream is a few wavy lines with a moving dash; sparks
 * are points running up. It moves and follows the charge, but the motion is periodic and the
 * rays are separate wedges, not a continuous field.
 */
@Composable
private fun GradientCharge(time: State<Float>, level: State<Float>, modifier: Modifier) {
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val blur = size.minDimension * 0.03f
                    renderEffect = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL)
                        .asComposeRenderEffect()
                }
                .drawWithCache {
                    val brushes = ChargeBrushes(size)
                    onDrawBehind { drawCharge(time.value, level.value, glow = true, brushes) }
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val brushes = ChargeBrushes(size)
                    onDrawBehind { drawCharge(time.value, level.value, glow = false, brushes) }
                },
        )
    }
}

/** Brushes built once per size; the per-frame drawing allocates only the moving dash. */
private class ChargeBrushes(size: Size) {
    val s = size.width
    val c = Offset(0.5f * s, RING_Y * size.height)
    val r = RING_R * s
    val arc = Brush.sweepGradient(listOf(Blue, Violet, Blue), c)
    // one ray pointing right; drawn rotated for each angle
    val ray = Brush.horizontalGradient(listOf(Violet, Color.Transparent), startX = c.x + r, endX = c.x + r + s * 0.22f)
}

private fun DrawScope.drawCharge(t: Float, level: Float, glow: Boolean, b: ChargeBrushes) {
    val s = b.s
    val c = b.c
    val r = b.r
    val ringWidth = s * if (glow) 0.03f else 0.012f

    // ring: dim full circle, bright arc clockwise from the bottom up to the level
    drawCircle(Blue.copy(alpha = 0.22f), r, c, style = Stroke(ringWidth))
    drawArc(
        b.arc, startAngle = 90f, sweepAngle = 360f * level, useCenter = false,
        topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = Stroke(ringWidth, cap = StrokeCap.Round),
    )
    // rays: wedges whose length flickers, longer as the charge grows
    for (i in 0 until RAYS) {
        val deg = i * 360f / RAYS
        val flicker = 0.5f + 0.25f * sin(t * 1.7f + i * 2.31f) + 0.25f * sin(t * 2.9f + i * 0.73f)
        val len = s * (0.04f + 0.18f * flicker * flicker * (0.35f + 0.9f * level))
        rotate(deg, c) {
            drawLine(
                b.ray, Offset(c.x + r, c.y), Offset(c.x + r + len, c.y),
                strokeWidth = s * if (glow) 0.03f else 0.008f, cap = StrokeCap.Round,
                alpha = 0.9f * flicker, blendMode = BlendMode.Plus,
            )
        }
    }
    // stream: wavy lines from the bottom edge to the ring, a dash running up them
    val dash = PathEffect.dashPathEffect(floatArrayOf(s * 0.05f, s * 0.03f), phase = -t * s * 0.6f)
    for (k in -2..2) {
        val x = c.x + k * s * 0.006f + sin(t * 3.1f + k) * s * 0.004f
        drawLine(
            Cyan.copy(alpha = if (k == 0) 1f else 0.45f),
            Offset(x, size.height), Offset(x, c.y + r),
            strokeWidth = s * (if (glow) 0.02f else 0.004f) * (0.5f + level),
            pathEffect = if (k == 0) null else dash,
            blendMode = BlendMode.Plus,
        )
    }
    // sparks running up the stream
    for (i in 0 until 40) {
        val u = (t * 0.9f + i * 0.137f) % 1f
        val x = c.x + sin(i * 12.9f) * s * 0.012f
        val y = size.height - u * (size.height - c.y - r)
        drawCircle(Color.White, s * 0.002f, Offset(x, y))
    }
}
