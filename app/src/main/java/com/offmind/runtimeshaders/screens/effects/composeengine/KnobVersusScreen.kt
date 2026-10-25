package com.offmind.runtimeshaders.screens.effects.composeengine

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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.screens.effects.metalknob.metalKnobShader
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.removeUniform
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// The knob from the Metal Knob demo, here lit by a lamp that circles slowly instead of following
// the phone's tilt, so both sides see exactly the same light every frame.
private const val KNOB_VALUE = 0.62f
private const val SWEEP = (1.5 * PI).toFloat()
private const val TICKS = 54
private val KnobBackground = Color(0xFFEAEAED)
private val Platform = Color(0xFFF0F0F2)
private val Steel = Color(0xFFCCD0D6)

/** Light direction in screen space (y down), normalized: circling at a fixed height. */
private fun lampDirection(angle: Float): FloatArray {
    val x = cos(angle) * 0.62f
    val y = sin(angle) * 0.62f
    val z = 0.78f
    val len = sqrt(x * x + y * y + z * z)
    return floatArrayOf(x / len, y / len, z / len)
}

private enum class KnobImpl(val label: String) {
    Shader("AGSL: brushed steel"),
    NoShader("Without a shader: gradients"),
}

@Composable
internal fun KnobVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(KnobImpl.Shader) }
    val angle = rememberInfiniteTransition(label = "lamp")
        .animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "a")
    val frames by rememberFrameSummary("screen=vs-knob variant=${impl.name}")
    val alloc = recordAllocations("vs-knob", impl.name)

    EngineScreen(
        title = "Brushed steel under a lamp",
        subtitle = "Every groove reflects the lamp its own way — per pixel, or approximated with gradients.",
    ) {
        EngineChoice("Implementation", KnobImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val box = Modifier.fillMaxWidth().aspectRatio(1f)
        when (impl) {
            KnobImpl.Shader -> ShaderKnob(angle, box)
            KnobImpl.NoShader -> GradientKnob(angle, box)
        }
        Text(
            if (impl == KnobImpl.Shader) "per pixel: groove tangent and lamp → anisotropic (Kajiya-Kay) highlights"
            else "repeating radial gradient for the brushing + a four-lobe sweep gradient turned to the lamp",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShaderKnob(angle: State<Float>, modifier: Modifier) {
    val shader = remember {
        Shader(metalKnobShader).getRuntimeShader(
            uniforms = basicUniformList
                .removeUniform("image")
                .addUniform(Uniform.Type.FLOAT to "knobRadius")
                .addUniform(Uniform.Type.FLOAT to "knobAngle")
                .addUniform(Uniform.Type.FLOAT to "knobValue")
                .addUniform(Uniform.Type.VEC3 to "lightDir"),
            customFunctions = emptySet(),
        )
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier.drawBehind {
            val l = lampDirection(angle.value)
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("knobRadius", size.minDimension * 0.30f)
            shader.setFloatUniform("knobAngle", (KNOB_VALUE - 0.5f) * SWEEP)
            shader.setFloatUniform("knobValue", KNOB_VALUE)
            shader.setFloatUniform("lightDir", l[0], l[1], l[2])
            drawRect(brush)
        },
    )
}

/**
 * The best attempt with built-in brushes: the same layout drawn with gradients. The brushing is a
 * radial gradient repeating every ring; the highlights are a four-lobe sweep gradient (lamp and
 * the fill light 90° away) turned towards the lamp; the rim is a sweep gradient too. It follows
 * the lamp's direction, but no ring reflects on its own, the lobes never bend with the grooves,
 * and the knurling and the notch walls do not catch the light.
 */
@Composable
private fun GradientKnob(angle: State<Float>, modifier: Modifier) {
    Box(
        modifier.drawWithCache {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.30f
            val face = r * 0.905f
            val brushing = Brush.radialGradient(
                0f to Steel.copy(red = Steel.red * 0.93f, green = Steel.green * 0.93f, blue = Steel.blue * 0.93f),
                0.5f to Steel,
                1f to Steel.copy(red = Steel.red * 0.93f, green = Steel.green * 0.93f, blue = Steel.blue * 0.93f),
                center = c, radius = 1.15f, tileMode = TileMode.Repeated,
            )
            val white = Color(0xFFF2F6FF)
            val lobes = Brush.sweepGradient(
                0.00f to white.copy(alpha = 0.75f), 0.035f to white.copy(alpha = 0.10f), 0.10f to Color.Transparent,
                0.18f to Color.Transparent, 0.235f to white.copy(alpha = 0.30f), 0.25f to white.copy(alpha = 0.40f),
                0.265f to white.copy(alpha = 0.30f), 0.32f to Color.Transparent,
                0.40f to Color.Transparent, 0.465f to white.copy(alpha = 0.10f), 0.50f to white.copy(alpha = 0.75f),
                0.535f to white.copy(alpha = 0.10f), 0.60f to Color.Transparent,
                0.68f to Color.Transparent, 0.735f to white.copy(alpha = 0.30f), 0.75f to white.copy(alpha = 0.40f),
                0.765f to white.copy(alpha = 0.30f), 0.82f to Color.Transparent,
                0.90f to Color.Transparent, 0.965f to white.copy(alpha = 0.10f), 1.00f to white.copy(alpha = 0.75f),
                center = c,
            )
            val rim = Brush.sweepGradient(
                0.00f to Color(0xFFE6E9F0), 0.15f to Color(0xFF8E9299), 0.25f to Color(0xFFB4B8C0),
                0.35f to Color(0xFF8E9299), 0.50f to Color(0xFF6C7077), 0.65f to Color(0xFF8E9299),
                0.75f to Color(0xFFB4B8C0), 0.85f to Color(0xFF8E9299), 1.00f to Color(0xFFE6E9F0),
                center = c,
            )
            val knurl = Path().apply {
                for (i in 0 until 160) {
                    val a = i / 160f * 2f * PI.toFloat()
                    moveTo(c.x + cos(a) * r * 0.80f, c.y + sin(a) * r * 0.80f)
                    lineTo(c.x + cos(a) * r * 0.883f, c.y + sin(a) * r * 0.883f)
                }
            }
            val knobAngleDeg = ((KNOB_VALUE - 0.5f) * SWEEP) * 180f / PI.toFloat()
            onDrawBehind {
                val l = lampDirection(angle.value)
                val lampDeg = atan2(l[1], l[0]) * 180f / PI.toFloat()
                val shadow = Offset(-l[0] / l[2], -l[1] / l[2])
                drawRect(KnobBackground)
                // platform shadow and platform
                val pc = c + shadow * r * 0.035f
                drawCircle(
                    Brush.radialGradient(
                        0.90f to Color.Black.copy(alpha = 0.07f), 1f to Color.Transparent, center = pc, radius = r * 1.40f,
                    ),
                    r * 1.40f, pc,
                )
                drawCircle(Platform, r * 1.26f, c)
                // ticks: 270°, the active ones dark
                for (i in 0..TICKS) {
                    val deg = -135f + 270f * i / TICKS
                    val major = i % 6 == 0
                    val active = i <= (KNOB_VALUE * TICKS).toInt()
                    rotate(deg, c) {
                        drawLine(
                            if (active) Color(0xFF29292E) else Color(0xFFA8A8AE),
                            Offset(c.x, c.y - r * (if (major) 1.36f else 1.40f)),
                            Offset(c.x, c.y - r * (if (major) 1.49f else 1.46f)),
                            strokeWidth = if (major) 2.6f else 1.5f,
                        )
                    }
                }
                // knob drop shadow: soft, shifted away from the lamp
                val sc = c + shadow * r * 0.11f
                drawCircle(
                    Brush.radialGradient(
                        0.80f to Color.Black.copy(alpha = 0.20f), 1f to Color.Transparent, center = sc, radius = r * 1.18f,
                    ),
                    r * 1.18f, sc,
                )
                // chrome rim, its bright side turned to the lamp
                rotate(lampDeg, c) { drawCircle(rim, r, c) }
                // face: brushed rings, then the four lobes turned to the lamp
                drawCircle(brushing, face, c)
                rotate(lampDeg, c) { drawCircle(lobes, face, c, blendMode = BlendMode.Plus) }
                // two machined grooves, the knurled band, the gap at the rim
                drawCircle(Color.Black.copy(alpha = 0.10f), r * 0.34f, c, style = Stroke(1.5f))
                drawCircle(Color.Black.copy(alpha = 0.10f), r * 0.58f, c, style = Stroke(1.2f))
                rotate(knobAngleDeg, c) { drawPath(knurl, Color.Black.copy(alpha = 0.10f), style = Stroke(1.6f)) }
                drawCircle(Color(0xFF3A3B40), face, c, style = Stroke(1.6f))
                // indicator notch
                rotate(knobAngleDeg, c) {
                    drawRoundRect(
                        Color(0xFF2A2B30),
                        topLeft = Offset(c.x - r * 0.016f, c.y - r * 0.74f),
                        size = Size(r * 0.032f, r * 0.15f),
                        cornerRadius = CornerRadius(r * 0.016f),
                    )
                }
                drawCircle(Color.Black.copy(alpha = 0.25f), r, c, style = Stroke(1.2f, cap = StrokeCap.Round))
            }
        },
    )
}
