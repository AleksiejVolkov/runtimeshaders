package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

// Geometry in dp, the same on both sides. A dark groove; inside it a smaller "active" pill that
// slides on its own (faster, shorter travel) and turns from orange to dark grey; on top a large
// frosted glass thumb that slides on its own, slower, past the groove's ends.
private val StageH = 200.dp
private val TrackW = 300.dp
private val TrackH = 96.dp
private val PillW = 128.dp
private val PillH = 64.dp
private val ThumbW = 214.dp
private val ThumbH = 112.dp
private val CapD = 84.dp
private val Orange = Color(0xFFFF7A12)
private val Amber = Color(0xFFFFB347)
private val PillOff = Color(0xFF34353B)

private fun Density.trackLeft(stageW: Float) = (stageW - TrackW.toPx()) / 2f

/** The thumb's left edge, px, for its progress 0 (off, left) … 1 (on, right). */
private fun Density.thumbLeft(p: Float, stageW: Float): Float {
    val from = trackLeft(stageW) - 10.dp.toPx()
    val to = trackLeft(stageW) + TrackW.toPx() - ThumbW.toPx() + 10.dp.toPx()
    return from + (to - from) * p
}

/** The active pill's left edge, px: a shorter path than the thumb's, on its own timing. */
private fun Density.pillLeft(p: Float, stageW: Float): Float {
    val from = trackLeft(stageW) + 16.dp.toPx()
    val to = trackLeft(stageW) + 74.dp.toPx() // on: peeks out from under the glass
    return from + (to - from) * p
}

private class ToggleLabels(val on: TextLayoutResult, val off: TextLayoutResult)

@Composable
private fun rememberToggleLabels(): ToggleLabels {
    val measurer = rememberTextMeasurer()
    return remember(measurer) {
        val style = TextStyle(color = Color(0xFFC9CBD2), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        ToggleLabels(measurer.measure("ON", style), measurer.measure("OFF", style))
    }
}

/** Groove, labels and the active pill with its glow — what the glass sits on, in stage px. */
private fun DrawScope.drawGroove(thumbP: Float, pillP: Float, labels: ToggleLabels, stage: Size = size) {
    val left = trackLeft(stage.width)
    val top = (stage.height - TrackH.toPx()) / 2f
    val r = TrackH.toPx() / 2f
    drawRoundRect(Color(0xFF141519), Offset(left, top), Size(TrackW.toPx(), TrackH.toPx()), CornerRadius(r))
    drawRoundRect(
        Color(0xFF3A3C44), Offset(left, top), Size(TrackW.toPx(), TrackH.toPx()), CornerRadius(r),
        style = Stroke(6.dp.toPx()),
    )
    val labelY = (stage.height - labels.on.size.height) / 2f
    drawText(labels.on, topLeft = Offset(left + 24.dp.toPx(), labelY), alpha = thumbP)
    drawText(
        labels.off,
        topLeft = Offset(left + TrackW.toPx() - 40.dp.toPx() - labels.off.size.width, labelY),
        alpha = 1f - thumbP,
    )
    // the active pill: orange and glowing when on, dark grey when off
    val pl = pillLeft(pillP, stage.width)
    val pt = (stage.height - PillH.toPx()) / 2f
    val pc = Offset(pl + PillW.toPx() / 2f, stage.height / 2f)
    val glow = 110.dp.toPx()
    drawCircle(
        Brush.radialGradient(listOf(Orange.copy(alpha = 0.55f * pillP), Color.Transparent), pc, glow),
        glow, pc,
    )
    drawRoundRect(
        Brush.horizontalGradient(
            listOf(lerp(PillOff, Amber, pillP), lerp(PillOff, Orange, pillP)), startX = pl, endX = pl + PillW.toPx(),
        ),
        Offset(pl, pt), Size(PillW.toPx(), PillH.toPx()), CornerRadius(PillH.toPx() / 2f),
    )
}

@Language("AGSL")
private const val GLASS_SHADER = """
uniform shader content;
uniform float4 thumb;     // left, top, right, bottom (px)
uniform float dp;         // px per dp

float box(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float sd(float2 p) {
    float2 c = (thumb.xy + thumb.zw) * 0.5;
    float2 h = (thumb.zw - thumb.xy) * 0.5;
    return box(p - c, h, h.y);
}

// 16 taps on a golden-angle spiral: a soft disk blur of what lies under the glass
half3 frosted(float2 q, float radius) {
    half3 acc = half3(0.0);
    for (int i = 0; i < 16; i++) {
        float a = float(i) * 2.39996;
        float rr = sqrt((float(i) + 0.5) / 16.0) * radius;
        acc += content.eval(q + float2(cos(a), sin(a)) * rr).rgb;
    }
    return acc / 16.0;
}

half4 main(float2 p) {
    half4 under = content.eval(p);
    float d = sd(p);
    if (d > 1.0) return under;

    float2 e = float2(1.0, 0.0);
    float2 n = normalize(float2(sd(p + e.xy) - sd(p - e.xy), sd(p + e.yx) - sd(p - e.yx)));
    float rim = 1.0 - clamp(-d / (12.0 * dp), 0.0, 1.0);
    // a thick sheet of glass: the rim bends what is under it a little towards the centre
    half3 glass = frosted(p - n * rim * rim * 8.0 * dp, 11.0 * dp);
    glass = mix(glass, half3(0.93, 0.93, 0.96), 0.13);
    float lit = dot(n, normalize(float2(-1.0, -1.3)));
    glass += half3(0.45) * half(rim * rim * max(lit, 0.0));
    glass += half3(0.35) * half(smoothstep(1.6 * dp, 0.0, -d));          // thin bright edge
    glass *= half(1.0 - rim * max(-lit, 0.0) * 0.25);

    float2 local = p - thumb.xy;
    float h = thumb.w - thumb.y;
    // raised dots on the leading part
    float2 cell = float2(16.0 * dp);
    float2 gc = (floor(local / cell) + 0.5) * cell;
    if (gc.x > 28.0 * dp && gc.x < 104.0 * dp && gc.y > 28.0 * dp && gc.y < h - 28.0 * dp) {
        float2 dd = local - gc;
        float dot_ = length(dd) - 3.2 * dp;
        float shade = dot(normalize(dd + 1e-3), normalize(float2(-1.0, -1.0)));
        glass += half3(0.22) * half(smoothstep(1.0, -1.0, dot_) * (0.45 + 0.55 * shade));
    }
    // the round cap at the trailing end: lighter glass with diagonal stripes
    float2 capC = float2(thumb.z - thumb.x - h * 0.5, h * 0.5);
    float capD = length(local - capC) - 42.0 * dp;
    if (capD < 0.0) {
        float stripes = 0.5 + 0.5 * sin((local.x + local.y) * 0.9 / dp);
        glass = mix(glass, half3(0.86, 0.80, 0.80), 0.28) + half3(0.05) * half(stripes);
    }
    glass += half3(0.18) * half(smoothstep(1.5 * dp, 0.0, abs(capD)));
    return mix(under, half4(glass, 1.0), half(clamp(0.5 - d, 0.0, 1.0)));
}
"""

private enum class ToggleImpl(val label: String) {
    Shader("AGSL: frosted glass"),
    NoShader("Without a shader: blurred copy"),
}

@Composable
internal fun GlassToggleVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(ToggleImpl.Shader) }
    val thumb = remember { Animatable(0f) }
    val pill = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        var on = false
        while (true) {
            delay(1_400)
            on = !on
            val target = if (on) 1f else 0f
            // two parts, two speeds: the pill in the groove is quicker than the glass above it
            launch { thumb.animateTo(target, tween(780, easing = FastOutSlowInEasing)) }
            launch { pill.animateTo(target, tween(420, easing = LinearOutSlowInEasing)) }
        }
    }
    val frames by rememberFrameSummary("screen=vs-toggle variant=${impl.name}")
    val alloc = recordAllocations("vs-toggle", impl.name)

    EngineScreen(
        title = "A glass toggle over light",
        subtitle = "The groove's pill and the glass thumb move at their own speeds; the glass frosts what is under it.",
    ) {
        EngineChoice("Implementation", ToggleImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val stage = Modifier.fillMaxWidth().height(StageH)
        when (impl) {
            ToggleImpl.Shader -> ShaderToggle(thumb.asState(), pill.asState(), stage)
            ToggleImpl.NoShader -> CopyToggle(thumb.asState(), pill.asState(), stage)
        }
        Text(
            if (impl == ToggleImpl.Shader) "RenderEffect: inside the thumb, read and blur what is underneath; bevel, dots, cap"
            else "the groove drawn again inside the clipped thumb, in a blurred layer; tint, rim, dots, cap on top",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShaderToggle(thumbP: State<Float>, pillP: State<Float>, modifier: Modifier) {
    val labels = rememberToggleLabels()
    val shader = remember { newRuntimeShader(GLASS_SHADER) }
    Box(
        modifier
            .graphicsLayer {
                val left = thumbLeft(thumbP.value, size.width)
                val top = (size.height - ThumbH.toPx()) / 2f
                shader.setFloatUniform("thumb", left, top, left + ThumbW.toPx(), top + ThumbH.toPx())
                shader.setFloatUniform("dp", 1.dp.toPx())
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }
            .drawBehind { drawGroove(thumbP.value, pillP.value, labels) },
    )
}

/**
 * The best version without a custom shader — what blur libraries such as Haze do: draw the content
 * under the glass a second time inside the clipped thumb, shifted to line up, in a layer blurred by
 * RenderEffect (wider than the thumb, so the edge has real neighbours to blur), then tint, rim,
 * dots and the striped cap on top. It frosts; it cannot bend light at the rim.
 */
@Composable
private fun CopyToggle(thumbP: State<Float>, pillP: State<Float>, modifier: Modifier) {
    val labels = rememberToggleLabels()
    var stage by remember { mutableStateOf(Size.Zero) }
    val margin = 30.dp
    Box(
        modifier
            .onSizeChanged { stage = Size(it.width.toFloat(), it.height.toFloat()) }
            .drawBehind { drawGroove(thumbP.value, pillP.value, labels) },
    ) {
        Box(
            Modifier
                .offset {
                    IntOffset(thumbLeft(thumbP.value, stage.width).toInt(), ((stage.height - ThumbH.toPx()) / 2f).toInt())
                }
                .size(ThumbW, ThumbH)
                .clip(RoundedCornerShape(50)),
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .requiredSize(ThumbW + margin * 2, ThumbH + margin * 2)
                    .graphicsLayer {
                        val blur = 11.dp.toPx() * 0.6f
                        renderEffect = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                    }
                    .drawBehind {
                        val left = thumbLeft(thumbP.value, stage.width) - margin.toPx()
                        val top = (stage.height - ThumbH.toPx()) / 2f - margin.toPx()
                        translate(-left, -top) { drawGroove(thumbP.value, pillP.value, labels, stage) }
                    },
            )
            Box(Modifier.fillMaxSize().drawWithCache {
                val tint = Color(0xFFEDEDF5).copy(alpha = 0.13f)
                val bevel = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.50f), Color.Transparent, Color.Black.copy(alpha = 0.20f)),
                    Offset.Zero, Offset(size.width * 0.3f, size.height),
                )
                val dot = 3.2.dp.toPx()
                val cell = 16.dp.toPx()
                val capR = CapD.toPx() / 2f
                val capC = Offset(size.width - size.height / 2f, size.height / 2f)
                val cap = Path().apply { addOval(androidx.compose.ui.geometry.Rect(capC, capR)) }
                onDrawBehind {
                    drawRect(tint)
                    drawRoundRect(bevel, cornerRadius = CornerRadius(size.height / 2f), style = Stroke(12.dp.toPx()))
                    drawRoundRect(
                        Color.White.copy(alpha = 0.35f), cornerRadius = CornerRadius(size.height / 2f),
                        style = Stroke(1.6.dp.toPx()),
                    )
                    var y = cell * 2.25f
                    while (y < size.height - 28.dp.toPx()) {
                        var x = cell * 2.25f
                        while (x < 104.dp.toPx()) {
                            drawCircle(Color.White.copy(alpha = 0.22f), dot, Offset(x, y))
                            x += cell
                        }
                        y += cell
                    }
                    clipPath(cap) {
                        drawCircle(Color(0xFFDBCCCC).copy(alpha = 0.28f), capR, capC)
                        var s = -capR * 2
                        while (s < capR * 2) {
                            drawLine(
                                Color.White.copy(alpha = 0.06f),
                                Offset(capC.x + s - capR, capC.y + capR), Offset(capC.x + s + capR, capC.y - capR),
                                strokeWidth = 3.5.dp.toPx(),
                            )
                            s += 7.dp.toPx()
                        }
                    }
                    drawCircle(Color.White.copy(alpha = 0.18f), capR, capC, style = Stroke(1.5.dp.toPx()))
                }
            })
        }
    }
}
