package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val BALLS = 6
private val BallColor = Color(0xFF5EE6C8)

/** Blob centres and radii as fractions of the box, moving along fixed Lissajous paths. */
private fun ballsAt(t: Float, w: Float, h: Float, out: FloatArray) {
    val s = min(w, h)
    for (i in 0 until BALLS) {
        val a = 2f * PI.toFloat() * (t + i / BALLS.toFloat())
        out[i * 4] = w / 2 + s * 0.30f * cos(a * (1 + i % 3))
        out[i * 4 + 1] = h / 2 + s * 0.30f * sin(a * (2 - i % 2) + i)
        out[i * 4 + 2] = s * (0.07f + 0.02f * (i % 3))
        out[i * 4 + 3] = 0f
    }
}

@Language("AGSL")
private const val METABALLS_SHADER = """
uniform float4 balls[6];
uniform float smoothness;
uniform half4 color;

float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

half4 main(float2 p) {
    float d = 1e5;
    for (int i = 0; i < 6; i++) {
        d = smin(d, length(p - balls[i].xy) - balls[i].z, smoothness);
    }
    return color * half(clamp(0.5 - d, 0.0, 1.0)); // one pixel of anti-aliasing
}
"""

@Composable
internal fun MetaballsVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(Impl.Shader) }
    val t = rememberInfiniteTransition(label = "balls")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "t")
    val frames by rememberFrameSummary("screen=vs-metaballs variant=${impl.name}")
    val alloc = recordAllocations("vs-metaballs", impl.name)

    EngineScreen(
        title = "Metaballs",
        subtitle = "One distance field, or blur + alpha threshold on an offscreen layer.",
    ) {
        EngineChoice("Implementation", Impl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            when (impl) {
                Impl.Shader -> ShaderMetaballs(t, Modifier.fillMaxWidth().aspectRatio(1f))
                Impl.NoShader -> BlurThresholdMetaballs(t, Modifier.fillMaxWidth().aspectRatio(1f))
            }
        }
        Text(
            if (impl == Impl.Shader) "smooth-min of six circle distances, one pass"
            else "circles → blur (RenderEffect) → ColorMatrix alpha threshold",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShaderMetaballs(t: State<Float>, modifier: Modifier) {
    val shader = remember {
        newRuntimeShader(METABALLS_SHADER).apply { setFloatUniform("color", 0.37f, 0.90f, 0.78f, 1f) }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    val balls = remember { FloatArray(BALLS * 4) }
    Box(
        modifier.drawBehind {
            ballsAt(t.value, size.width, size.height, balls)
            shader.setFloatUniform("balls", balls)
            shader.setFloatUniform("smoothness", size.minDimension * 0.08f)
            drawRect(brush)
        },
    )
}

/**
 * The classic "gooey" effect without a custom shader: draw sharp circles, blur the whole layer, then
 * stretch alpha with a color matrix so that everything above half opacity becomes solid.
 */
@Composable
private fun BlurThresholdMetaballs(t: State<Float>, modifier: Modifier) {
    val balls = remember { FloatArray(BALLS * 4) }
    Box(
        modifier
            .graphicsLayer {
                val blur = size.minDimension * 0.035f
                // alpha' = 20 · alpha − 10: the blurred edge at alpha 0.5 becomes the new hard edge.
                val threshold = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 20f, -10f * 255f,
                    ),
                )
                renderEffect = RenderEffect.createColorFilterEffect(
                    ColorMatrixColorFilter(threshold),
                    RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL),
                ).asComposeRenderEffect()
            }
            .drawBehind {
                ballsAt(t.value, size.width, size.height, balls)
                for (i in 0 until BALLS) {
                    drawCircle(BallColor, balls[i * 4 + 2], Offset(balls[i * 4], balls[i * 4 + 1]))
                }
            },
    )
}
