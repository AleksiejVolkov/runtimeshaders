package com.offmind.runtimeshaders.screens.effects.speedometer

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// 0 km/h at about 8 o'clock, 240 at about 4, clockwise: in screen degrees (y down) that is
// 150° → 390°.
private const val StartDeg = 150f
private const val SweepDeg = 240f
private const val InnerBand = 0.75f
private const val RedlineFrom = 200f
private const val HoldAtTop = 1.2f // s, before the launch repeats

private enum class HueMode(val label: String) { Cycle("hue: time"), Speed("hue: speed") }

@Composable
fun SpeedometerScreen() {
    var time by remember { mutableFloatStateOf(0f) } // scenario time
    var clock by remember { mutableFloatStateOf(0f) } // keeps the smoke moving even on pause
    var playing by remember { mutableStateOf(true) }
    var hueMode by remember { mutableStateOf(HueMode.Cycle) }
    var light by remember { mutableStateOf(true) } // light (OKLCH, tonemap) or paint (HSV, mix)
    var aberration by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1e9f
                    clock += dt
                    if (playing) {
                        time += dt
                        if (time > ScenarioLength + HoldAtTop) time = 0f
                    }
                }
                last = now
            }
        }
    }

    val speed = speedAt(time)
    val fillSpeed = fillSpeedAt(time)
    val hue = when (hueMode) {
        HueMode.Cycle -> cycleHueAt(time, light)
        HueMode.Speed -> speedHue(fillSpeed, light)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Speedometer(
            speed = speed,
            fill = fillSpeed / MaxSpeed,
            hue = hue,
            light = light,
            exposure = exposureAt(fillSpeed),
            aberration = if (aberration) aberrationAt(speed) else 0f,
            clock = clock,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().aspectRatio(1f),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (playing) "pause" else "play") { playing = !playing }
                listOf(63, 112, 170, 220).forEach { target ->
                    Chip("$target") {
                        playing = false
                        time = timeOfSpeed(target.toFloat())
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(hueMode.label) { hueMode = HueMode.entries[(hueMode.ordinal + 1) % 2] }
                Chip(if (light) "light" else "paint") { light = !light }
                Chip(if (aberration) "CA on" else "CA off") { aberration = !aberration }
            }
        }
    }
}

@Composable
private fun Speedometer(
    speed: Float,
    fill: Float,
    hue: Float,
    light: Boolean,
    exposure: Float,
    aberration: Float,
    clock: Float,
    modifier: Modifier,
) {
    val shader = remember { RuntimeShader(speedFillShader) }
    val split = remember { RuntimeShader(aberrationShader) }
    val brush = remember { ShaderBrush(shader) }
    val text = rememberTextMeasurer()

    // feeds the fill shader; both fill layers draw the same thing
    fun DrawScope.drawFill(alpha: Float, blendMode: BlendMode) {
        val radius = dialRadius()
        shader.setFloatUniform("center", center.x, center.y)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("inner", InnerBand)
        shader.setFloatUniform("startAngle", Math.toRadians(StartDeg.toDouble()).toFloat())
        shader.setFloatUniform("sweep", Math.toRadians(SweepDeg.toDouble()).toFloat())
        shader.setFloatUniform("fill", fill)
        shader.setFloatUniform("hue", hue / 360f)
        shader.setFloatUniform("time", clock)
        shader.setFloatUniform("model", if (light) 1f else 0f)
        shader.setFloatUniform("exposure", exposure)
        drawRect(brush, alpha = alpha, blendMode = blendMode)
    }

    Box(
        modifier.graphicsLayer {
            // past the redline everything on the dial splits, scale and needle included;
            // below it the layer has no effect at all
            renderEffect = if (aberration > 0f) {
                val pulse = 1f + 0.15f * sin(clock * 2f * PI.toFloat() * 1.5f)
                split.setFloatUniform("center", size.width / 2f, size.height / 2f)
                split.setFloatUniform("amount", 0.02f * aberration * pulse)
                RenderEffect.createRuntimeShaderEffect(split, "image").asComposeRenderEffect()
            } else {
                null
            }
        }
    ) {
        // bloom: the same fill, blurred in its own layer
        Canvas(
            Modifier
                .matchParentSize()
                .graphicsLayer { renderEffect = BlurEffect(10.dp.toPx(), 10.dp.toPx(), TileMode.Decal) }
        ) { drawFill(alpha = 0.55f, blendMode = BlendMode.SrcOver) }
        // the sharp fill, added on top of its glow
        Canvas(Modifier.matchParentSize()) { drawFill(alpha = 1f, blendMode = BlendMode.Plus) }
        // scale, labels, redline, needle and the number stay crisp and white over the glow
        Canvas(Modifier.matchParentSize()) {
            drawDial(speed, fill, tint(hue + RedlineHueOffset, light), text)
        }
    }
}

private fun DrawScope.dialRadius() = min(size.width, size.height) * 0.42f

private fun DrawScope.pointAt(angleDeg: Float, r: Float): Offset {
    val a = Math.toRadians(angleDeg.toDouble())
    return Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
}

private fun angleOf(speed: Float) = StartDeg + SweepDeg * (speed / MaxSpeed).coerceIn(0f, 1f)

// The colour of the redline and the charge segment, in the same model as the fill.
private fun tint(hue: Float, light: Boolean): Color =
    if (light) oklch(0.66f, 0.17f, hue) else Color.hsv(wrapHue(hue), 0.9f, 1f)

private fun oklch(l: Float, c: Float, hueDeg: Float): Color {
    val h = Math.toRadians(wrapHue(hueDeg).toDouble())
    val a = c * cos(h).toFloat()
    val b = c * sin(h).toFloat()
    fun cube(x: Float) = x * x * x
    val l1 = cube(l + 0.3963377774f * a + 0.2158037573f * b)
    val m1 = cube(l - 0.1055613458f * a - 0.0638541728f * b)
    val s1 = cube(l - 0.0894841775f * a - 1.2914855480f * b)
    fun encode(x: Float) = Math.pow(x.coerceIn(0f, 1f).toDouble(), 1 / 2.2).toFloat()
    return Color(
        encode(4.0767416621f * l1 - 3.3077115913f * m1 + 0.2309699292f * s1),
        encode(-1.2684380046f * l1 + 2.6097574011f * m1 - 0.3413193965f * s1),
        encode(-0.0041960863f * l1 - 0.7034186147f * m1 + 1.7076147010f * s1),
    )
}

private fun DrawScope.drawDial(speed: Float, fill: Float, redlineTint: Color, text: TextMeasurer) {
    val r = dialRadius()
    val arcTopLeft = Offset(center.x - r, center.y - r)
    val arcSize = Size(2 * r, 2 * r)

    // the rim, brighter over the filled part
    drawArc(Color.White.copy(alpha = 0.35f), StartDeg, SweepDeg, false, arcTopLeft, arcSize, style = Stroke(1.5.dp.toPx()))
    drawArc(Color.White, StartDeg, SweepDeg * fill, false, arcTopLeft, arcSize, style = Stroke(1.5.dp.toPx()))

    // ticks: a major one every 20 km/h, four minor ones in between
    for (i in 0..60) {
        val major = i % 5 == 0
        val angle = StartDeg + SweepDeg * i / 60f
        drawLine(
            Color.White.copy(alpha = if (major) 0.95f else 0.55f),
            pointAt(angle, r * (if (major) 0.9f else 0.945f)),
            pointAt(angle, r * 0.985f),
            strokeWidth = (if (major) 2.5.dp else 1.2.dp).toPx(),
            cap = StrokeCap.Butt,
        )
    }

    // labels inside the ticks
    val labelStyle = TextStyle(
        color = Color.White,
        fontSize = (r * 0.085f / density / fontScale).sp,
        fontWeight = FontWeight.Medium,
    )
    for (value in 0..240 step 20) {
        val layout = text.measure(value.toString(), labelStyle)
        val at = pointAt(angleOf(value.toFloat()), r * 0.8f)
        drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
    }

    // redline outside the scale, in the complementary colour, with a faint glow
    val red = redlineTint
    val redR = r * 1.07f
    val redTopLeft = Offset(center.x - redR, center.y - redR)
    val redSize = Size(2 * redR, 2 * redR)
    val redStart = angleOf(RedlineFrom)
    val redSweep = angleOf(MaxSpeed) - redStart
    drawArc(red.copy(alpha = 0.18f), redStart, redSweep, false, redTopLeft, redSize, style = Stroke(10.dp.toPx()))
    drawArc(red.copy(alpha = 0.45f), redStart, redSweep, false, redTopLeft, redSize, style = Stroke(4.dp.toPx()))
    drawArc(red, redStart, redSweep, false, redTopLeft, redSize, style = Stroke(2.dp.toPx()))

    // needle: a thin line from the centre circle to the rim, at the real speed
    val needle = angleOf(speed)
    drawLine(Color.White, pointAt(needle, r * 0.3f), pointAt(needle, r * 0.985f), strokeWidth = 1.5.dp.toPx())

    // centre: a thin circle, the number and the unit
    drawCircle(Color.White.copy(alpha = 0.85f), radius = r * 0.3f, style = Stroke(1.5.dp.toPx()))
    val number = text.measure(
        speed.roundToInt().toString(),
        TextStyle(color = Color.White, fontSize = (r * 0.2f / density / fontScale).sp, fontWeight = FontWeight.Medium),
    )
    drawText(number, topLeft = center - Offset(number.size.width / 2f, number.size.height * 0.62f))
    val unit = text.measure("km/h", TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = (r * 0.065f / density / fontScale).sp))
    drawText(unit, topLeft = center + Offset(-unit.size.width / 2f, r * 0.1f))
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1C1C1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
