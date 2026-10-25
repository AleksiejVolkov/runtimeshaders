package com.offmind.runtimeshaders.screens.effects.composeengine.hero

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.MutableState
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.screens.effects.composeengine.EngineChoice
import com.offmind.runtimeshaders.screens.effects.composeengine.PhaseCounter
import com.offmind.runtimeshaders.screens.effects.composeengine.RecordPhases
import com.offmind.runtimeshaders.screens.effects.composeengine.blurPassSource
import com.offmind.runtimeshaders.screens.effects.composeengine.newRuntimeShader
import com.offmind.runtimeshaders.screens.effects.composeengine.recordAllocations
import com.offmind.runtimeshaders.screens.effects.composeengine.rememberFrameSummary
import org.intellij.lang.annotations.Language
import kotlin.math.ceil

// The film's hero screen: in-car navigation. A procedural map moves under the finger (or drives
// itself along the route); the route header blurs the map more towards the top edge; glass cards
// for the next turns bend the roads under them; a brushed-steel climate knob sets the temperature.

private val HeaderHeight = 210.dp
private val HeaderBlur = 22.dp
private val CardCorner = 26.dp
private const val MAX_CARDS = 4
private const val WORLD_PER_DP = 1.05f   // map scale: world units per dp

internal enum class HeaderBlurMode(val label: String) { Shader("Blur: AGSL"), Stack("Blur: stack"), None("Blur: none") }

/*
 * The glass of the cards: Snell's law through a thick slab with a rounded bevel, as in the glass
 * refraction demo (glassrefraction/BackdropGlass.kt, `glassShader`, by the same author), applied to
 * the map layer with the cards' rectangles as uniforms.
 */
@Language("AGSL")
private val CardGlassShader = """
    uniform shader image;
    uniform float4 panels[$MAX_CARDS];
    uniform float cornerRadii[$MAX_CARDS];
    uniform float panelCount;
    uniform float thickness;
    uniform float bevel;
    uniform float ior;

    float sdRoundedBox(float2 p, float2 halfSize, float radius) {
        float2 q = abs(p) - halfSize + radius;
        return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;
    }

    float2 outwardAt(float2 p, float2 halfSize, float radius) {
        float2 dx = float2(1.0, 0.0);
        float2 dy = float2(0.0, 1.0);
        return normalize(float2(
            sdRoundedBox(p + dx, halfSize, radius) - sdRoundedBox(p - dx, halfSize, radius),
            sdRoundedBox(p + dy, halfSize, radius) - sdRoundedBox(p - dy, halfSize, radius)
        ) + 1e-5);
    }

    float glassHeight(float x) { return thickness * sqrt(1.0 - x * x); }
    float glassSlope(float x, float b) { return thickness / b * x / sqrt(max(1.0 - x * x, 1e-3)); }

    half4 glassOver(float2 xy, float4 panel, float cornerRadius) {
        float2 halfSize = panel.zw * 0.5;
        float2 p = xy - panel.xy - halfSize;
        float sd = sdRoundedBox(p, halfSize, cornerRadius);
        float coverage = clamp(0.5 - sd, 0.0, 1.0);
        if (coverage == 0.0) return half4(0.0);
        float b = min(bevel, min(halfSize.x, halfSize.y));
        float glassRadius = max(cornerRadius, b);
        float sdGlass = sdRoundedBox(p, halfSize, glassRadius);
        float2 outward = outwardAt(p, halfSize, glassRadius);
        float x = clamp(1.0 + sdGlass / b, 0.0, 1.0);
        float3 normal = normalize(float3(outward * glassSlope(x, b), 1.0));
        float3 ray = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / ior);
        float2 shift = ray.xy * glassHeight(x) / -ray.z;
        half3 color = image.eval(xy + shift).rgb;
        color = mix(color, half3(1.0), 0.18);
        return half4(color, 1.0) * coverage;
    }

    half4 main(float2 xy) {
        for (int i = 0; i < $MAX_CARDS; i++) {
            if (float(i) >= panelCount) break;
            half4 color = glassOver(xy, panels[i], cornerRadii[i]);
            if (color.a > 0.0) return color;
        }
        return half4(0.0);
    }
"""

/** Where the glass cards are, in root pixels; written by the cards and the map, read by the map. */
private class CardRects {
    val rects = Array(MAX_CARDS) { mutableStateOf(Rect.Zero) }
    val mapOrigin = mutableStateOf(Offset.Zero)
}

@Composable
internal fun NavHeroScreen() {
    var blurMode by rememberSaveable { mutableStateOf(HeaderBlurMode.Shader) }
    var knobRead by rememberSaveable { mutableStateOf(KnobRead.Draw) }
    var drive by rememberSaveable { mutableStateOf(false) }
    var spin by rememberSaveable { mutableStateOf(false) }
    var lab by rememberSaveable { mutableStateOf(false) }

    val temperature = remember { mutableFloatStateOf(0.45f) }
    val knobCounter = remember { PhaseCounter("climate-knob") }
    val pan = remember { mutableStateOf(Offset(-300f, 0f)) }
    val routeDone = remember { mutableFloatStateOf(0f) }
    val cards = remember { CardRects() }

    // auto-drive: the car follows the route, the map moves under it
    LaunchedEffect(drive) {
        if (!drive) return@LaunchedEffect
        var last = 0L
        var d = routeDone.floatValue
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) d = (d + (now - last) / 1e9f * 60f) % 2600f   // 60 m/s, for the camera
                last = now
                val (qx, qy) = routePoint(d)
                val (wx, wy) = unwarp(qx, qy)
                pan.value = Offset(wx, wy)
                routeDone.floatValue = d
            }
        }
    }
    // auto-spin: the knob sweeps back and forth, for measuring the three read variants
    val spinValue = rememberInfiniteTransition(label = "spin")
        .animateFloat(0.15f, 0.85f, infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Reverse), label = "v")
    if (spin) LaunchedEffect(Unit) { while (true) withFrameNanos { temperature.floatValue = spinValue.value } }

    val variant = "blur=${blurMode.name}/knob=${knobRead.name}/drive=$drive/spin=$spin"
    RecordPhases("hero-nav", variant, listOf(knobCounter))
    rememberFrameSummary("screen=hero-nav variant=$variant")
    recordAllocations("hero-nav", variant)

    Box(Modifier.fillMaxSize().background(Color(0xFFF2F0EA))) {
        NavMapLayer(pan, routeDone, blurMode, cards, Modifier.fillMaxSize())

        RouteHeader(Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(20.dp))

        Column(
            Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard(cards, 0, "↰  1.2 km", "Ring Road")
            GlassCard(cards, 1, "18 min", "arrive 14:32")
        }

        Column(
            Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TemperatureLabel(temperature)
            ClimateKnob(temperature, knobRead, knobCounter)
        }

        // the lab: measurement switches, hidden behind a small button
        Text(
            "⋯",
            color = Color(0xFF6B6F78),
            fontSize = 22.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(8.dp)
                .clip(CircleShape)
                .clickable { lab = !lab }
                .padding(horizontal = 14.dp, vertical = 4.dp),
        )
        if (lab) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE111318))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EngineChoice("Header blur", HeaderBlurMode.entries, blurMode, { it.label }) { blurMode = it }
                EngineChoice("Knob angle", KnobRead.entries, knobRead, { it.label }) { knobRead = it }
                EngineChoice("Drive", listOf(false, true), drive, { if (it) "Drive: on" else "Drive: off" }) { drive = it }
                EngineChoice("Spin", listOf(false, true), spin, { if (it) "Spin: on" else "Spin: off" }) { spin = it }
                Text("close", color = Color(0xFF8A90A3), fontSize = 12.sp, modifier = Modifier.clickable { lab = false })
            }
        }
    }
}

private val focusFraction = Offset(0.5f, 0.56f)

@Composable
private fun RouteHeader(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("In 300 m", color = Color(0xFF1C2230), fontSize = 16.sp)
        Text("turn left onto Mill Street", color = Color(0xFF1C2230), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TemperatureLabel(temperature: State<Float>) {
    // recomposes only when the shown half-degree changes
    val shown by remember { derivedStateOf { (16f + temperature.value * 12f) * 2f } }
    val text = "%.1f°".format(java.util.Locale.US, kotlin.math.round(shown) / 2f)
    Text(text, color = Color(0xFF1C2230), fontSize = 22.sp, fontWeight = FontWeight.Medium)
}

/** A card that draws only its text: the glass under it is drawn by the map layer. */
@Composable
private fun GlassCard(cards: CardRects, index: Int, title: String, subtitle: String) {
    Column(
        Modifier
            .width(210.dp)
            .height(84.dp)
            .onGloballyPositioned { cards.rects[index].value = Rect(it.positionInRoot(), it.size.toSize()) }
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(CardCorner))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Color(0xFF1C2230), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Color(0xFF3A4152), fontSize = 14.sp)
    }
}

/**
 * The map: one AGSL pass over the whole screen. Its layer carries the header blur (the
 * BlurRadiusSpec shader, two passes) and the glass of the cards (the Snell shader), as one
 * RenderEffect; in the stack mode the header blur is instead a few blurred copies on top.
 */
@Composable
private fun NavMapLayer(
    pan: MutableState<Offset>,
    routeDone: State<Float>,
    blurMode: HeaderBlurMode,
    cards: CardRects,
    modifier: Modifier,
) {
    val map = remember { newRuntimeShader(NAV_MAP_SHADER) }
    val mapBrush = remember(map) { ShaderBrush(map) }
    val glass = remember { newRuntimeShader(CardGlassShader) }
    val hPass = remember { newRuntimeShader(blurPassSource(vertical = false)) }
    val vPass = remember { newRuntimeShader(blurPassSource(vertical = true)) }
    val mapLayer = rememberGraphicsLayer()

    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { cards.mapOrigin.value = it.positionInRoot() }
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        pan.value -= drag * (WORLD_PER_DP / density)   // the map moves under the finger
                    }
                }
                .graphicsLayer {
                    // the glass of the cards, in this layer's pixels
                    val origin = cards.mapOrigin.value
                    val bounds = FloatArray(MAX_CARDS * 4)
                    val radii = FloatArray(MAX_CARDS)
                    var n = 0
                    for (r in cards.rects) {
                        val rect = r.value.translate(-origin)
                        if (rect.width <= 0f) continue
                        bounds[n * 4] = rect.left; bounds[n * 4 + 1] = rect.top
                        bounds[n * 4 + 2] = rect.width; bounds[n * 4 + 3] = rect.height
                        radii[n] = CardCorner.toPx()
                        n++
                    }
                    glass.setFloatUniform("panels", bounds)
                    glass.setFloatUniform("cornerRadii", radii)
                    glass.setFloatUniform("panelCount", n.toFloat())
                    glass.setFloatUniform("thickness", 40.dp.toPx())
                    glass.setFloatUniform("bevel", 60.dp.toPx())
                    glass.setFloatUniform("ior", 1.5f)

                    val source: RenderEffect? = if (blurMode == HeaderBlurMode.Shader) {
                        for (s in listOf(hPass, vPass)) {
                            s.setFloatUniform("blurRadius", HeaderBlur.toPx())
                            s.setFloatUniform("crop", 0f, 0f, size.width, size.height)
                            s.setFloatUniform("startIntensity", 1f)
                            s.setFloatUniform("endIntensity", 0f)
                            s.setFloatUniform("startPoint", 0f, 0f)
                            s.setFloatUniform("endPoint", 0f, HeaderHeight.toPx())
                        }
                        RenderEffect.createChainEffect(
                            RenderEffect.createRuntimeShaderEffect(vPass, "content"),
                            RenderEffect.createRuntimeShaderEffect(hPass, "content"),
                        )
                    } else {
                        null
                    }
                    val frost = RenderEffect.createBlurEffect(4.dp.toPx(), 4.dp.toPx(), Shader.TileMode.CLAMP)
                    val cardGlass = RenderEffect.createChainEffect(
                        RenderEffect.createRuntimeShaderEffect(glass, "image"),
                        frost,
                    )
                    val under = source ?: RenderEffect.createOffsetEffect(0f, 0f)
                    renderEffect = RenderEffect.createBlendModeEffect(under, cardGlass, BlendMode.SRC_OVER)
                        .asComposeRenderEffect()
                }
                .drawWithContent {
                    mapLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(mapLayer)
                }
                .drawBehind {
                    val scale = density / WORLD_PER_DP
                    map.setFloatUniform("size", size.width, size.height)
                    map.setFloatUniform("pan", pan.value.x, pan.value.y)
                    map.setFloatUniform("focus", size.width * focusFraction.x, size.height * focusFraction.y)
                    map.setFloatUniform("scale", scale)
                    map.setFloatUniform("routeDone", routeDone.value)
                    drawRect(mapBrush)
                },
        )
        // the car, at the screen's focus, where the route passes when driving
        Box(
            Modifier.fillMaxSize().drawBehind {
                val c = Offset(size.width * focusFraction.x, size.height * focusFraction.y)
                drawCircle(Color.Black.copy(alpha = 0.12f), 17.dp.toPx(), c + Offset(0f, 2.dp.toPx()))
                drawCircle(Color.White, 14.dp.toPx(), c)
                drawCircle(Color(0xFF2F6BF0), 10.dp.toPx(), c)
            },
        )
        if (blurMode == HeaderBlurMode.Stack) HeaderBlurStack(mapLayer)
    }
}

/**
 * The header blur without a custom shader, as Haze does it below API 33: copies of the recorded
 * map over the header, each blurred with one platform radius and masked to a band (64 dp steps).
 */
@Composable
private fun HeaderBlurStack(mapLayer: GraphicsLayer) {
    val steps = ceil(HeaderHeight.value / 64f).toInt()
    for (i in 0..steps) {
        val fraction = 1f - i / steps.toFloat()
        if (fraction <= 0f) continue
        Box(
            Modifier
                .fillMaxWidth()
                .height(HeaderHeight)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val mask = Brush.verticalGradient(
                        ((i - 2f) / steps).coerceIn(0f, 1f) to (if (i <= 1) Color.Black else Color.Transparent),
                        ((i - 1f) / steps).coerceIn(0f, 1f) to Color.Black,
                        (i / steps.toFloat()).coerceIn(0f, 1f) to Color.Black,
                        ((i + 1f) / steps).coerceIn(0f, 1f) to Color.Transparent,
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(mask, blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                    }
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val r = HeaderBlur.toPx() * fraction
                        renderEffect = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    }
                    .drawBehind { drawLayer(mapLayer) },
            )
        }
    }
}
