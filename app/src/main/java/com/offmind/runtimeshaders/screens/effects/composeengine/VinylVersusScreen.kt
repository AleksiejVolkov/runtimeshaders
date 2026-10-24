package com.offmind.runtimeshaders.screens.effects.composeengine

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Proportions of the record, as fractions of its radius.
private const val LABEL = 0.34f      // the paper label
private const val TRACK = 0.11f      // width of one track; the smooth gaps between tracks show as rings
private const val GROOVE_PX = 3.2f   // groove pitch, px

/**
 * The lamp: a point light gliding on an ellipse that passes over the record off-centre, fairly
 * low, so that both its direction and its distance change. Returns x, y, height in px.
 */
private fun lampAt(t: Float, cx: Float, cy: Float, r: Float): Triple<Float, Float, Float> {
    val a = 2f * PI.toFloat() * t
    return Triple(cx + r * 0.85f * cos(a), cy + r * 0.25f + r * 0.55f * sin(a), r * 0.55f)
}

@Language("AGSL")
private const val VINYL_SHADER = """
uniform float2 center;
uniform float radius;
uniform float3 lamp;      // point light: x, y, height (px)

float hash(float n) { return fract(sin(n * 12.9898) * 43758.5453); }

half4 main(float2 p) {
    float2 d = p - center;
    float r = length(d);
    float edge = clamp(radius - r, 0.0, 1.0);
    if (edge <= 0.0 || r < radius * 0.34) return half4(0.0);
    float2 radial = d / r;
    // The groove runs around the centre: its tangent is perpendicular to the radius.
    float3 tangent = float3(-radial.y, radial.x, 0.0);
    // Tracks are grooved; the narrow lands between tracks are smooth and catch no sheen.
    float track = fract((r / radius - 0.34) / 0.11);
    float grooved = smoothstep(0.0, 0.025, track) * smoothstep(1.0, 0.975, track);

    // One groove every 3.2 px, a V in cross-section: the inner wall faces the centre, the outer
    // wall faces out. Each groove has its own depth, like the loudness of the music cut into it.
    float g = r / 3.2;
    float across = fract(g);
    float level = 0.45 + 0.55 * hash(floor(g));
    float wall = smoothstep(0.0, 0.2, across) - 2.0 * smoothstep(0.4, 0.6, across) + smoothstep(0.8, 1.0, across);
    float slope = wall * 0.55 * level * grooved;
    float3 n = normalize(float3(radial * slope, 1.0));

    float3 toLamp = lamp - float3(p, 0.0);
    float dist = length(toLamp);
    float3 l = toLamp / dist;
    float3 h = normalize(l + float3(0.0, 0.0, 1.0));       // viewer straight above
    float falloff = radius * radius / (radius * radius + dist * dist * 0.6);
    // Anisotropic sheen (Kajiya-Kay): bright where the groove tangent is perpendicular to h —
    // a band through the centre, pointing at the lamp, that bends and narrows as the lamp moves.
    float th = dot(tangent, h);
    float sheen = pow(sqrt(max(1.0 - th * th, 0.0)), 90.0) * grooved;
    // Inside the band each wall either catches the lamp or not: the band is striped by grooves.
    float walls = pow(max(dot(n, h), 0.0), 24.0);
    float band = sheen * (0.25 + 1.1 * walls) * (0.6 + 0.4 * level);
    // A dim light from above everywhere, so the grooves read even away from the band.
    float fill = max(dot(n, normalize(float3(0.0, -0.6, 1.0))), 0.0);
    half3 c = half3(0.016, 0.016, 0.019) + half3(0.035, 0.035, 0.04) * half(fill * fill)
            + half3(0.82, 0.85, 0.92) * half(band * falloff)
            + half3(0.05) * half(max(dot(n, l), 0.0) * falloff);
    return half4(c * half(edge), half(edge));
}
"""

private enum class VinylImpl(val label: String) {
    Shader("AGSL: grooves + point lamp"),
    NoShader("Without a shader: gradients"),
}

@Composable
internal fun VinylVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(VinylImpl.Shader) }
    val transition = rememberInfiniteTransition(label = "vinyl")
    val lampT = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(7_000, easing = LinearEasing)), label = "lamp")
    // 33⅓ rpm
    val spin = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1_800, easing = LinearEasing)), label = "spin")
    val frames by rememberFrameSummary("screen=vs-vinyl variant=${impl.name}")
    val alloc = recordAllocations("vs-vinyl", impl.name)

    EngineScreen(
        title = "A vinyl record under a lamp",
        subtitle = "Concentric grooves catch the light as a band that follows the lamp.",
    ) {
        EngineChoice("Implementation", VinylImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            val disc = Modifier.fillMaxSize()
            when (impl) {
                VinylImpl.Shader -> ShaderVinyl(lampT, disc)
                VinylImpl.NoShader -> GradientVinyl(lampT, disc)
            }
            RecordLabel(spin)
        }
        Text(
            if (impl == VinylImpl.Shader) "per pixel: groove tangent, lamp direction and distance → anisotropic sheen"
            else "repeating radial gradient for grooves + a sweep-gradient sheen turned towards the lamp",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

/** The paper label, spinning; the same on both sides. */
@Composable
private fun RecordLabel(spin: State<Float>) {
    Box(
        Modifier
            .fillMaxWidth(LABEL * 0.94f)
            .aspectRatio(1f)
            .graphicsLayer { rotationZ = spin.value }
            .clip(CircleShape)
            .background(Color(0xFFD9481E)),
        contentAlignment = Alignment.Center,
    ) {
        Text("SIDE A", color = Color(0xFFFFE8D6), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF111111)))
    }
}

@Composable
private fun ShaderVinyl(lampT: State<Float>, modifier: Modifier) {
    val shader = remember { newRuntimeShader(VINYL_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier.drawBehind {
            val r = size.minDimension / 2f * 0.98f
            val (lx, ly, lh) = lampAt(lampT.value, center.x, center.y, r)
            shader.setFloatUniform("center", center.x, center.y)
            shader.setFloatUniform("radius", r)
            shader.setFloatUniform("lamp", lx, ly, lh)
            drawRect(brush)
        },
    )
}

/**
 * The best attempt without a shader. Grooves: a radial gradient repeating at the groove pitch.
 * Track gaps: thin rings. Sheen: a sweep gradient with two bright sectors, turned every frame so
 * that one of them points at the lamp. It follows the lamp's direction, but not its distance or
 * position: the band never bends, narrows or brightens as the lamp passes over the record.
 */
@Composable
private fun GradientVinyl(lampT: State<Float>, modifier: Modifier) {
    Box(
        modifier.drawWithCache {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f * 0.98f
            val grooves = Brush.radialGradient(
                0f to Color(0xFF050506), 0.5f to Color(0xFF26262B), 1f to Color(0xFF050506),
                center = c,
                radius = GROOVE_PX,
                tileMode = TileMode.Repeated,
            )
            val sheen = Brush.sweepGradient(
                0.00f to Color.White.copy(alpha = 0.55f),
                0.02f to Color.White.copy(alpha = 0.25f),
                0.06f to Color.Transparent,
                0.44f to Color.Transparent,
                0.48f to Color.White.copy(alpha = 0.25f),
                0.50f to Color.White.copy(alpha = 0.55f),
                0.52f to Color.White.copy(alpha = 0.25f),
                0.56f to Color.Transparent,
                0.94f to Color.Transparent,
                0.98f to Color.White.copy(alpha = 0.25f),
                1.00f to Color.White.copy(alpha = 0.55f),
                center = c,
            )
            val gaps = (1..5).map { k -> r * (LABEL + TRACK * k) }
            onDrawBehind {
                drawCircle(grooves, r, center)
                val (lx, ly, _) = lampAt(lampT.value, center.x, center.y, r)
                val angle = atan2(ly - center.y, lx - center.x) * 180f / PI.toFloat()
                rotate(angle, center) { drawCircle(sheen, r, center, blendMode = BlendMode.Plus) }
                gaps.forEach { drawCircle(Color(0xFF050505), it, center, style = Stroke(r * 0.008f)) }
            }
        },
    )
}
