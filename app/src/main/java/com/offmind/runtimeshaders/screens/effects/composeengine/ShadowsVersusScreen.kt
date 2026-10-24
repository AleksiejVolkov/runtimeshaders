package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

private val Surface = Color(0xFFE0E5EC)
private val DarkShadow = Color(0xFFA3B1C6)
private val LightShadow = Color(0xFFFFFFFF)

// The shape: a rounded five-point star, as an exact signed distance (Inigo Quilez's sdStar5) —
// the same function in Kotlin (to build the Path) and in AGSL. Sizes are fractions of the box.
private const val STAR_R = 0.36f
private const val STAR_RF = 0.55f
private const val STAR_ROUND = 0.03f

private fun starDistance(px: Float, py0: Float): Float {
    val k1x = 0.809016994375f
    val k1y = -0.587785252292f
    val k2x = -k1x
    var x = abs(px)
    var y = -py0 // screen y points down; the star points up
    var d = max(k1x * x + k1y * y, 0f)
    x -= 2f * d * k1x; y -= 2f * d * k1y
    d = max(k2x * x + k1y * y, 0f)
    x -= 2f * d * k2x; y -= 2f * d * k1y
    x = abs(x)
    y -= STAR_R
    val bax = STAR_RF * -k1y
    val bay = STAR_RF * k1x - 1f
    val h = ((x * bax + y * bay) / (bax * bax + bay * bay)).coerceIn(0f, STAR_R)
    return hypot(x - bax * h, y - bay * h) * sign(y * bax - x * bay) - STAR_ROUND
}

/** The outline of the zero distance, found along 360 rays from the centre (the star is star-shaped). */
private fun starPath(size: Float): Path {
    val path = Path()
    for (i in 0 until 360) {
        val a = i / 360f * 2f * PI.toFloat()
        var lo = 0f
        var hi = 0.6f
        repeat(24) {
            val mid = (lo + hi) / 2
            if (starDistance(mid * cos(a), mid * sin(a)) < 0f) lo = mid else hi = mid
        }
        val x = size / 2 + lo * size * cos(a)
        val y = size / 2 + lo * size * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Language("AGSL")
private const val SHADOWS_SHADER = """
uniform float boxSize;
uniform float2 light;     // unit vector of the shadow offset (away from the light)
uniform float offsetPx;   // shadow offset, in px
uniform float softness;   // shadow blur, in px
uniform half4 surface;
uniform half4 dark;
uniform half4 bright;

float star(float2 p) {
    const float2 k1 = float2(0.809016994375, -0.587785252292);
    const float2 k2 = float2(-0.809016994375, -0.587785252292);
    p.x = abs(p.x);
    p.y = -p.y;
    p -= 2.0 * max(dot(k1, p), 0.0) * k1;
    p -= 2.0 * max(dot(k2, p), 0.0) * k2;
    p.x = abs(p.x);
    p.y -= 0.36;
    float2 ba = 0.55 * float2(-k1.y, k1.x) - float2(0.0, 1.0);
    float h = clamp(dot(p, ba) / dot(ba, ba), 0.0, 0.36);
    return length(p - ba * h) * sign(p.y * ba.x - p.x * ba.y) - 0.03;
}

// Distance in pixels, from the centre of the box.
float sd(float2 p) { return star((p - boxSize * 0.5) / boxSize) * boxSize; }

// Coverage of a shape blurred by `softness`, from its distance.
float blurred(float d) { return 1.0 - smoothstep(-softness, softness, d); }

half4 main(float2 p) {
    float2 off = light * offsetPx;
    float d = sd(p);
    // outside: the shape's highlight shifted towards the light, its shadow away from it
    half4 outside = mix(surface, bright, half(blurred(sd(p + off)) * 0.9));
    outside = mix(outside, dark, half(blurred(sd(p - off)) * 0.8));
    // inside: the shadow of "everything but the shape", shifted — an inner shadow on the far edge
    half4 inside = mix(surface, dark, half(blurred(-sd(p - off * 0.6)) * 0.45));
    return mix(outside, inside, half(clamp(0.5 - d, 0.0, 1.0)));
}
"""

@Composable
internal fun ShadowsVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(Impl.Shader) }
    val angle = rememberInfiniteTransition(label = "light")
        .animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "a")
    val frames by rememberFrameSummary("screen=vs-shadows variant=${impl.name}")
    val alloc = recordAllocations("vs-shadows", impl.name)

    EngineScreen(
        title = "Soft shadows, moving light",
        subtitle = "A neumorphic star lit from a turning light: distance field, or blurred masks.",
    ) {
        EngineChoice("Implementation", Impl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val box = Modifier.fillMaxWidth().aspectRatio(1f).background(Surface)
        when (impl) {
            Impl.Shader -> ShaderShadows(angle, box)
            Impl.NoShader -> BlurMaskShadows(angle, box)
        }
        Text(
            if (impl == Impl.Shader) "one pass: the star's distance, sampled three times"
            else "BlurMaskFilter paths (outer dark + light) and a clipped, blurred inverse path (inner)",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShaderShadows(angle: State<Float>, modifier: Modifier) {
    val shader = remember {
        newRuntimeShader(SHADOWS_SHADER).apply {
            setFloatUniform("surface", 0.878f, 0.898f, 0.925f, 1f)
            setFloatUniform("dark", 0.639f, 0.694f, 0.776f, 1f)
            setFloatUniform("bright", 1f, 1f, 1f, 1f)
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier.drawBehind {
            val s = size.minDimension
            shader.setFloatUniform("boxSize", s)
            shader.setFloatUniform("light", cos(angle.value), sin(angle.value))
            shader.setFloatUniform("offsetPx", s * 0.035f)
            // BlurMaskFilter(radius) has sigma ≈ 0.58 · radius; ±2 sigma ≈ the same visual softness.
            shader.setFloatUniform("softness", s * 0.04f * 1.15f)
            drawRect(brush)
        },
    )
}

/**
 * What a careful Compose developer writes without a custom shader: platform paints with a
 * BlurMaskFilter for the outer shadows, and for the inner shadow a clip to the star plus a blurred
 * "everything except the star" path, all shifted by the light every frame. Paths and paints are
 * built once; nothing is allocated while drawing.
 */
@Composable
private fun BlurMaskShadows(angle: State<Float>, modifier: Modifier) {
    val cache = remember { BlurShadowCache() }
    Box(
        modifier.drawBehind {
            val s = size.minDimension
            cache.prepare(s)
            val off = s * 0.035f
            val dx = cos(angle.value) * off
            val dy = sin(angle.value) * off
            drawIntoCanvas { canvas ->
                val c = canvas.nativeCanvas
                c.save(); c.translate(-dx, -dy); c.drawPath(cache.star, cache.brightPaint); c.restore()
                c.save(); c.translate(dx, dy); c.drawPath(cache.star, cache.darkPaint); c.restore()
                c.drawPath(cache.star, cache.fillPaint)
                c.save()
                c.clipPath(cache.star)
                c.translate(dx * 0.6f, dy * 0.6f)
                c.drawPath(cache.inverse, cache.innerPaint)
                c.restore()
            }
        },
    )
}

private class BlurShadowCache {
    private var builtFor = -1f
    var star = Path()
    var inverse = Path()
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Surface.toArgb() }
    val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DarkShadow.copy(alpha = 0.8f).toArgb() }
    val brightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LightShadow.copy(alpha = 0.9f).toArgb() }
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DarkShadow.copy(alpha = 0.45f).toArgb() }

    fun prepare(size: Float) {
        if (size == builtFor) return
        builtFor = size
        star = starPath(size)
        inverse = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(RectF(-size, -size, 2 * size, 2 * size), Path.Direction.CW)
            addPath(star)
        }
        val blur = BlurMaskFilter(size * 0.04f, BlurMaskFilter.Blur.NORMAL)
        darkPaint.maskFilter = blur
        brightPaint.maskFilter = blur
        innerPaint.maskFilter = BlurMaskFilter(size * 0.03f, BlurMaskFilter.Blur.NORMAL)
    }
}
