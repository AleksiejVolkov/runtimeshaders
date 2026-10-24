package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language

private val FadeHeight = 72.dp

/** The fade curve: a cubic ease (smoothstep), 0 at the very edge, 1 at [FadeHeight] in. */
private fun fadeCurve(x: Float) = x * x * (3f - 2f * x)

private enum class FadeImpl(val label: String) {
    Shader("AGSL shader"),
    Gradient("Gradient, 12 stops"),
    Linear("Gradient, 2 stops"),
}

@Language("AGSL")
private const val EDGE_FADE_SHADER = """
uniform shader content;
uniform float height;
uniform float fade;    // px

half4 main(float2 p) {
    // distance to the nearer edge, 0…1 inside the fade band, then the same cubic curve
    float x = clamp(min(p.y, height - p.y) / fade, 0.0, 1.0);
    return content.eval(p) * half(x * x * (3.0 - 2.0 * x));
}
"""

@Composable
internal fun EdgeFadeVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(FadeImpl.Shader) }
    val t = rememberInfiniteTransition(label = "scroll")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "t")
    val frames by rememberFrameSummary("screen=vs-edgefade variant=${impl.name}")
    val alloc = recordAllocations("vs-edgefade", impl.name)

    EngineScreen(
        title = "Fade the edges of a list",
        subtitle = "Each row dissolves only where it crosses the edge, with a cubic falloff.",
    ) {
        EngineChoice("Implementation", FadeImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val box = Modifier.fillMaxWidth().height(440.dp).clipToBounds()
        when (impl) {
            FadeImpl.Shader -> ShaderEdgeFade(t, box)
            FadeImpl.Gradient -> GradientEdgeFade(t, box, stops = 12)
            FadeImpl.Linear -> GradientEdgeFade(t, box, stops = 2)
        }
        Text(
            when (impl) {
                FadeImpl.Shader -> "RenderEffect: alpha = smoothstep of the distance to the edge"
                FadeImpl.Gradient -> "offscreen layer + a vertical gradient mask (BlendMode.DstIn), the cubic curve in 12 stops"
                FadeImpl.Linear -> "the same mask with two stops: a straight line, not the cubic curve"
            },
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

/** A list that scrolls by itself, read in placement. */
@Composable
private fun FadingList(t: State<Float>) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top, unbounded = true)
            .offset { IntOffset(0, -((t.value * 4f % 1f) * 12 * 76.dp.toPx()).toInt()) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(30) { i ->
            val hue = (i % 12 * 30).toFloat()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.hsv(hue, 0.5f, 0.55f))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.85f)))
                Text("Track ${i % 12 + 1} · fades where it meets the edge", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ShaderEdgeFade(t: State<Float>, modifier: Modifier) {
    val shader = remember { newRuntimeShader(EDGE_FADE_SHADER) }
    Box(
        modifier.graphicsLayer {
            shader.setFloatUniform("height", size.height)
            shader.setFloatUniform("fade", FadeHeight.toPx())
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        },
    ) { FadingList(t) }
}

/**
 * Without a shader: draw the list into an offscreen layer, then multiply its alpha by a vertical
 * gradient (BlendMode.DstIn). A gradient interpolates linearly between stops, so the cubic curve
 * is approximated by [stops] samples of it; two stops give a plain linear fade.
 */
@Composable
private fun GradientEdgeFade(t: State<Float>, modifier: Modifier, stops: Int) {
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                // built once per size, not every frame
                val f = (FadeHeight.toPx() / size.height).coerceAtMost(0.5f)
                val colorStops = buildList {
                    for (k in 0 until stops) {
                        val x = k / (stops - 1f)
                        add(x * f to Color.Black.copy(alpha = if (stops == 2) x else fadeCurve(x)))
                    }
                    for (k in stops - 1 downTo 0) {
                        val x = k / (stops - 1f)
                        add(1f - x * f to Color.Black.copy(alpha = if (stops == 2) x else fadeCurve(x)))
                    }
                }.toTypedArray()
                val mask = Brush.verticalGradient(*colorStops)
                onDrawWithContent {
                    drawContent()
                    drawRect(mask, blendMode = BlendMode.DstIn)
                }
            },
    ) { FadingList(t) }
}
