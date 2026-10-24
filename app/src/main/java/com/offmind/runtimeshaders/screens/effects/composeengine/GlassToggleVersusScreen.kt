package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
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
import org.intellij.lang.annotations.Language

// Geometry in dp, the same on both sides. The glass thumb is taller than the track and slides
// along it; the orange light sits under the thumb's leading half and travels with it.
private val StageH = 200.dp
private val TrackW = 300.dp
private val TrackH = 96.dp
private val ThumbW = 200.dp
private val ThumbH = 108.dp
private val Orange = Color(0xFFFF7A1A)

/** The thumb's left edge inside the stage, px, for slide progress 0 (off) … 1 (on). */
private fun Density.thumbLeft(progress: Float, stageW: Float): Float {
    val trackLeft = (stageW - TrackW.toPx()) / 2f
    val from = trackLeft - 8.dp.toPx()
    val to = trackLeft + TrackW.toPx() - ThumbW.toPx() + 8.dp.toPx()
    return from + (to - from) * progress
}

/** Track, label and the orange light — the content the glass sits on, in stage coordinates. */
private fun DrawScope.drawToggleContent(progress: Float, label: TextLayoutResult, stage: Size = size) {
    val trackLeft = (stage.width - TrackW.toPx()) / 2f
    val trackTop = (stage.height - TrackH.toPx()) / 2f
    val r = TrackH.toPx() / 2f
    drawRoundRect(Color(0xFF16171B), Offset(trackLeft, trackTop), Size(TrackW.toPx(), TrackH.toPx()), CornerRadius(r))
    drawRoundRect(
        Color(0xFF3A3C44), Offset(trackLeft, trackTop), Size(TrackW.toPx(), TrackH.toPx()), CornerRadius(r),
        style = Stroke(6.dp.toPx()),
    )
    drawText(label, topLeft = Offset(trackLeft + 40.dp.toPx(), (stage.height - label.size.height) / 2f))
    // the light: an orange pill and a wide soft glow, centred on the thumb's leading third
    val thumbX = thumbLeft(progress, stage.width)
    val lightC = Offset(thumbX + ThumbW.toPx() * 0.28f, stage.height / 2f)
    drawCircle(
        Brush.radialGradient(listOf(Orange.copy(alpha = 0.85f), Orange.copy(alpha = 0.25f), Color.Transparent), lightC, 120.dp.toPx()),
        120.dp.toPx(), lightC,
    )
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFB066), Orange, Orange.copy(alpha = 0f)), lightC, 46.dp.toPx()),
        46.dp.toPx(), lightC,
    )
}

@Language("AGSL")
private const val GLASS_SHADER = """
uniform shader content;
uniform float4 thumb;     // left, top, right, bottom (px)
uniform float corner;     // corner radius (px)
uniform float dp;         // px per dp

float box(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float sd(float2 p) {
    float2 c = (thumb.xy + thumb.zw) * 0.5;
    return box(p - c, (thumb.zw - thumb.xy) * 0.5, corner);
}

half4 frosted(float2 q, float radius) {
    // 16 taps on a golden-angle spiral: a soft disk blur of what is underneath
    half4 acc = half4(0.0);
    for (int i = 0; i < 16; i++) {
        float a = float(i) * 2.39996;
        float rr = sqrt((float(i) + 0.5) / 16.0) * radius;
        acc += content.eval(q + float2(cos(a), sin(a)) * rr);
    }
    return acc / 16.0;
}

half4 main(float2 p) {
    half4 under = content.eval(p);
    float d = sd(p);
    if (d > 1.0) return under;

    // The rim is a lens: near the edge the glass bends the light behind it towards the centre,
    // a little more for blue than for red.
    float2 e = float2(1.0, 0.0);
    float2 n = normalize(float2(sd(p + e.xy) - sd(p - e.xy), sd(p + e.yx) - sd(p - e.yx)));
    float rim = 1.0 - clamp(-d / (16.0 * dp), 0.0, 1.0);
    float bend = rim * rim * 22.0 * dp;
    float blur = 9.0 * dp;
    half r = frosted(p - n * bend * 0.92, blur).r;
    half g = frosted(p - n * bend, blur).g;
    half b = frosted(p - n * bend * 1.08, blur).b;
    half3 glass = half3(r, g, b);

    // frost tint, a bevel lit from the top left, a darker lower edge
    glass = mix(glass, half3(0.92, 0.92, 0.95), 0.10);
    float light = dot(n, normalize(float2(-1.0, -1.2)));
    glass += half3(0.55) * half(rim * rim * max(light, 0.0));
    glass *= half(1.0 - rim * rim * max(-light, 0.0) * 0.35);

    // a grid of raised dots on the leading half
    float2 local = p - thumb.xy;
    float2 cell = float2(16.0 * dp);
    float2 gc = (floor(local / cell) + 0.5) * cell;
    if (gc.x > 30.0 * dp && gc.x < 105.0 * dp && gc.y > 30.0 * dp && gc.y < (thumb.w - thumb.y) - 30.0 * dp) {
        float2 dd = local - gc;
        float dot_ = length(dd) - 3.2 * dp;
        float shade = dot(normalize(dd + 1e-3), normalize(float2(-1.0, -1.0)));
        glass += half3(0.25) * half(smoothstep(1.0, -1.0, dot_) * (0.4 + 0.6 * shade));
    }
    float edge = clamp(0.5 - d, 0.0, 1.0);
    half4 outC = half4(glass, 1.0);
    return mix(under, outC, half(edge));
}
"""

private enum class ToggleImpl(val label: String) {
    Shader("AGSL: frosted lens"),
    NoShader("Without a shader: blurred copy"),
}

@Composable
internal fun GlassToggleVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(ToggleImpl.Shader) }
    val progress = rememberInfiniteTransition(label = "toggle").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1_600, delayMillis = 700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "slide",
    )
    val frames by rememberFrameSummary("screen=vs-toggle variant=${impl.name}")
    val alloc = recordAllocations("vs-toggle", impl.name)

    EngineScreen(
        title = "A glass toggle over light",
        subtitle = "Frosted glass slides over an orange glow; the light moves underneath it.",
    ) {
        EngineChoice("Implementation", ToggleImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val stage = Modifier.fillMaxWidth().height(StageH)
        when (impl) {
            ToggleImpl.Shader -> ShaderToggle(progress, stage)
            ToggleImpl.NoShader -> CopyToggle(progress, stage)
        }
        Text(
            if (impl == ToggleImpl.Shader) "RenderEffect: inside the thumb, read the light underneath — blurred, bent at the rim"
            else "the same light drawn again inside the clipped thumb, in a blurred layer; tint, rim and dots on top",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShaderToggle(progress: State<Float>, modifier: Modifier) {
    val label = rememberToggleLabel()
    val shader = remember { newRuntimeShader(GLASS_SHADER) }
    Box(
        modifier
            .graphicsLayer {
                val left = thumbLeft(progress.value, size.width)
                val top = (size.height - ThumbH.toPx()) / 2f
                shader.setFloatUniform("thumb", left, top, left + ThumbW.toPx(), top + ThumbH.toPx())
                shader.setFloatUniform("corner", ThumbH.toPx() / 2f)
                shader.setFloatUniform("dp", 1.dp.toPx())
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }
            .drawBehind { drawToggleContent(progress.value, label) },
    )
}

@Composable
private fun rememberToggleLabel(): TextLayoutResult {
    val measurer = rememberTextMeasurer()
    return remember(measurer) {
        measurer.measure("ON", TextStyle(color = Color(0xFFC9CBD2), fontSize = 26.sp, fontWeight = FontWeight.SemiBold))
    }
}

/**
 * The best version without a custom shader: the frosted look comes from drawing the same light a
 * second time inside the thumb — clipped to its shape, shifted so it lines up, in a layer blurred
 * by RenderEffect — plus a white tint, a gradient rim and dots on top. It blurs, but it cannot
 * bend: the rim does not act as a lens and there are no colored fringes.
 */
@Composable
private fun CopyToggle(progress: State<Float>, modifier: Modifier) {
    val label = rememberToggleLabel()
    var stage by remember { mutableStateOf(Size.Zero) }
    Box(
        modifier
            .onSizeChanged { stage = Size(it.width.toFloat(), it.height.toFloat()) }
            .drawBehind { drawToggleContent(progress.value, label) },
    ) {
        Box(
            Modifier
                .offset {
                    val left = thumbLeft(progress.value, stage.width)
                    IntOffset(left.toInt(), ((stage.height - ThumbH.toPx()) / 2f).toInt())
                }
                .size(ThumbW, ThumbH)
                .clip(RoundedCornerShape(50))
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.15f))),
                    RoundedCornerShape(50),
                ),
        ) {
            // The blurred copy: the stage's light drawn again, shifted back under the thumb, in a
            // layer larger than the thumb so the blur has real neighbours at the edge; the thumb's
            // clip trims it.
            val margin = 30.dp
            Box(
                Modifier
                    .align(Alignment.Center)
                    .requiredSize(ThumbW + margin * 2, ThumbH + margin * 2)
                    .graphicsLayer {
                        val blur = 9.dp.toPx()
                        renderEffect = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                    }
                    .drawBehind {
                        val left = thumbLeft(progress.value, stage.width) - margin.toPx()
                        val top = (stage.height - ThumbH.toPx()) / 2f - margin.toPx()
                        translate(-left, -top) { drawToggleContent(progress.value, label, stage) }
                    },
            )
            // tint, bevel, dots
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val tint = Color(0xFFEBEBF2).copy(alpha = 0.10f)
                        val bevel = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                            Offset.Zero, Offset(size.width * 0.35f, size.height),
                        )
                        val dot = 3.2.dp.toPx()
                        val cell = 16.dp.toPx()
                        onDrawBehind {
                            drawRect(tint)
                            drawRoundRect(bevel, cornerRadius = CornerRadius(size.height / 2f), style = Stroke(14.dp.toPx()))
                            var y = cell * 2.5f
                            while (y < size.height - 30.dp.toPx()) {
                                var x = cell * 2.5f
                                while (x < 105.dp.toPx()) {
                                    drawCircle(Color.White.copy(alpha = 0.22f), dot, Offset(x, y))
                                    x += cell
                                }
                                y += cell
                            }
                        }
                    },
            )
        }
    }
}
