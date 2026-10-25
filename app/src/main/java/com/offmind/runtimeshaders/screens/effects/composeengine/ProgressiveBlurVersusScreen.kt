package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language
import kotlin.math.min

// A list scrolls under a header; the header blurs the list progressively: 24 dp at the top edge,
// nothing at the header's bottom edge.
private val ListHeight = 460.dp
private val HeaderHeight = 200.dp
private val MaxBlur = 24.dp

// Haze's layered approximation steps every 64 dp of the gradient (BlurRenderEffectVisualEffect.kt,
// drawProgressiveWithMultipleLayers): a 200 dp header is 4 steps, 5 copies.
private val HazeSteps = kotlin.math.ceil(HeaderHeight.value / 64f).toInt()

/*
 * The variable-radius pass of Compose's BlurRadiusSpec (androidx.compose.ui:ui-graphics
 * 1.13.0-beta02, blur/BlurShaders.kt, progressiveLinearBlurSkSl + blurCoreSkSl; Apache License 2.0,
 * Copyright 2026 The Android Open Source Project), here without the Decal branch. A separable
 * Gaussian whose radius at each pixel is the maximum times an intensity that falls along a line;
 * two programs, one per axis, chained like BlurRadiusSpec does on API 33+.
 */
private fun blurPassSource(vertical: Boolean): String {
    val offset = if (vertical) "vec2(0.0, i + weightH / weight)" else "vec2(i + weightH / weight, 0.0)"
    val odd = if (vertical) "vec2(0.0, r)" else "vec2(r, 0.0)"
    val inBounds = if (vertical) {
        "step(crop.y, c.y) * (1.0 - step(crop.w, c.y))"
    } else {
        "step(crop.x, c.x) * (1.0 - step(crop.z, c.x))"
    }
    return """
uniform shader content;
uniform float blurRadius;
uniform float4 crop;
const float maxRadius = 150.0;
uniform float startIntensity;
uniform float endIntensity;
uniform float2 startPoint;
uniform float2 endPoint;

float gaussian(float x, float sigma) { return exp(-(x * x) / (2.0 * sigma * sigma)); }
float inside(vec2 c) { return $inBounds; }

vec4 blur(vec2 coord, float radius) {
    float r = floor(radius);
    if (r < 1.0) { return content.eval(coord); }
    float sigma = max(radius / 2.0, 1.0);
    float weightSum = 1.0;
    vec4 result = content.eval(coord);
    for (float i = 1.0; i < maxRadius; i += 2.0) {
        if (i >= r) { break; }
        float weightL = gaussian(i, sigma);
        float weightH = gaussian(i + 1.0, sigma);
        float weight = weightL + weightH;
        vec2 offset = $offset;          // two taps in one read, between the pair
        vec2 c1 = coord - offset;
        float m1 = inside(c1);
        weightSum += weight * m1;
        if (m1 > 0.0) { result += weight * content.eval(c1); }
        vec2 c2 = coord + offset;
        float m2 = inside(c2);
        weightSum += weight * m2;
        if (m2 > 0.0) { result += weight * content.eval(c2); }
    }
    float oddWeight = gaussian(r, sigma) * mod(r, 2.0);
    vec2 o = $odd;
    float m3 = inside(coord - o);
    weightSum += oddWeight * m3;
    if (m3 > 0.0) { result += oddWeight * content.eval(coord - o); }
    float m4 = inside(coord + o);
    weightSum += oddWeight * m4;
    if (m4 > 0.0) { result += oddWeight * content.eval(coord + o); }
    return result / weightSum;
}

half4 main(float2 coord) {
    float2 pa = coord - startPoint;
    float2 ba = endPoint - startPoint;
    float fraction = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);
    return half4(blur(coord, blurRadius * mix(startIntensity, endIntensity, fraction)));
}
"""
}

@Language("AGSL")
private val HorizontalPass = blurPassSource(vertical = false)

@Language("AGSL")
private val VerticalPass = blurPassSource(vertical = true)

private enum class BlurImpl(val label: String) {
    Shader("AGSL (BlurRadiusSpec's shader)"),
    Stack("Without a shader: blur stack"),
    Uniform("One radius (reference)"),
    None("No blur (zero)"),
}

@Composable
internal fun ProgressiveBlurVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(BlurImpl.Shader) }
    val t = rememberInfiniteTransition(label = "scroll")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "t")
    val frames by rememberFrameSummary("screen=vs-progblur variant=${impl.name}")
    val alloc = recordAllocations("vs-progblur", impl.name)

    EngineScreen(
        title = "A header that blurs progressively",
        subtitle = "The list under the header blurs more towards the top edge.",
    ) {
        EngineChoice("Implementation", BlurImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        Box(Modifier.fillMaxWidth().height(ListHeight).clipToBounds()) {
            when (impl) {
                BlurImpl.Shader -> ShaderProgressiveBlur(t)
                BlurImpl.Stack -> StackProgressiveBlur(t, steps = HazeSteps)
                BlurImpl.Uniform -> StackProgressiveBlur(t, steps = 1)
                BlurImpl.None -> StackProgressiveBlur(t, steps = 0)
            }
            HeaderTitle()
        }
        Text(
            when (impl) {
                BlurImpl.Shader -> "one RenderEffect chain over the list: horizontal then vertical blur, radius per pixel"
                BlurImpl.Stack -> "the list recorded once, drawn again in bands of 64 dp (as Haze), each copy with one platform blur radius and a band mask"
                BlurImpl.Uniform -> "the same, with one band and one radius: cheap, but the blur does not grow towards the edge"
                BlurImpl.None -> "the same screen without any blur: the baseline"
            },
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun HeaderTitle() {
    Text(
        "Inbox",
        color = Color.White,
        fontSize = 30.sp,
        modifier = Modifier.padding(start = 20.dp, top = 56.dp),
    )
}

/** The live content: a list of colourful rows that keeps scrolling (read in placement). */
@Composable
private fun BlurredList(t: State<Float>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top, unbounded = true)
            .offset { IntOffset(0, -((t.value * 3f % 1f) * 10 * 92.dp.toPx()).toInt()) },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(24) { i ->
            val hue = (i % 10 * 36).toFloat()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color.hsv(hue, 0.7f, 0.85f), Color.hsv((hue + 40) % 360, 0.6f, 0.45f))))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f)))
                Text("Message ${i % 10 + 1} · sharp text to blur", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ShaderProgressiveBlur(t: State<Float>) {
    val horizontal = remember { newRuntimeShader(HorizontalPass) }
    val vertical = remember { newRuntimeShader(VerticalPass) }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val max = min(MaxBlur.toPx(), 150f)
                for (s: RuntimeShader in listOf(horizontal, vertical)) {
                    s.setFloatUniform("blurRadius", max)
                    s.setFloatUniform("crop", 0f, 0f, size.width, size.height)
                    s.setFloatUniform("startIntensity", 1f)
                    s.setFloatUniform("endIntensity", 0f)
                    s.setFloatUniform("startPoint", 0f, 0f)
                    s.setFloatUniform("endPoint", 0f, HeaderHeight.toPx())
                }
                renderEffect = RenderEffect.createChainEffect(
                    RenderEffect.createRuntimeShaderEffect(vertical, "content"),
                    RenderEffect.createRuntimeShaderEffect(horizontal, "content"),
                ).asComposeRenderEffect()
            },
    ) { BlurredList(t) }
}

/**
 * Without a custom shader, the way Haze does it where runtime shaders are unavailable
 * (`drawProgressiveWithMultipleLayers`): the list is recorded into a GraphicsLayer once per frame
 * and drawn sharp; then it is drawn again [steps] + 1 times, each copy blurred with one platform
 * radius sampled from the ramp, and masked — after the blur — to a band: opaque over two steps,
 * fading over one step on each side, so neighbouring copies overlap and blend. More steps look
 * smoother and cost more.
 */
@Composable
private fun StackProgressiveBlur(t: State<Float>, steps: Int) {
    val listLayer = rememberGraphicsLayer()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    listLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(listLayer)
                },
        ) { BlurredList(t) }
        if (steps == 0) {
            // no blur: the baseline
        } else if (steps == 1) {
            BlurBand(listLayer, radiusFraction = 0.5f) {
                Brush.verticalGradient(0f to Color.Black, 0.85f to Color.Black, 1f to Color.Transparent)
            }
        } else {
            for (i in 0..steps) {
                val f = i / steps.toFloat()
                BlurBand(listLayer, radiusFraction = 1f - f) {
                    Brush.verticalGradient(
                        ((i - 2f) / steps).coerceIn(0f, 1f) to (if (i <= 1) Color.Black else Color.Transparent),
                        ((i - 1f) / steps).coerceIn(0f, 1f) to Color.Black,
                        (i / steps.toFloat()).coerceIn(0f, 1f) to Color.Black,
                        ((i + 1f) / steps).coerceIn(0f, 1f) to Color.Transparent,
                    )
                }
            }
        }
    }
}

/** One copy of the list over the header, blurred by [radiusFraction] of the maximum, then masked. */
@Composable
private fun BlurBand(listLayer: GraphicsLayer, radiusFraction: Float, mask: () -> Brush) {
    if (radiusFraction <= 0f) return
    Box(
        Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val brush = mask()
                onDrawWithContent {
                    drawContent()
                    drawRect(brush, blendMode = BlendMode.DstIn) // the mask, over the blurred copy
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val radius = MaxBlur.toPx() * radiusFraction
                    renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .drawBehind { drawLayer(listLayer) },
        )
    }
}
