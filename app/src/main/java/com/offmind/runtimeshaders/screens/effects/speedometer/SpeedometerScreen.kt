package com.offmind.runtimeshaders.screens.effects.speedometer

import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        HueMode.Cycle -> cycleHueAt(time)
        HueMode.Speed -> speedHue(fillSpeed)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Speedometer(
            speed = speed,
            fill = fillSpeed / MaxSpeed,
            hue = hue,
            clock = clock,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().aspectRatio(1f),
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(if (playing) "pause" else "play") { playing = !playing }
            listOf(63, 112, 170).forEach { target ->
                Chip("$target") {
                    playing = false
                    time = timeOfSpeed(target.toFloat())
                }
            }
            Chip(hueMode.label) { hueMode = HueMode.entries[(hueMode.ordinal + 1) % 2] }
        }
    }
}

@Composable
private fun Speedometer(speed: Float, fill: Float, hue: Float, clock: Float, modifier: Modifier) {
    val shader = remember { RuntimeShader(speedFillShader) }
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
        drawRect(brush, alpha = alpha, blendMode = blendMode)
    }

    Box(modifier) {
        // the hub sits under everything
        Canvas(Modifier.matchParentSize()) { drawHub(hue, text) }
        // bloom: the same fill, blurred in its own layer
        Canvas(
            Modifier
                .matchParentSize()
                .graphicsLayer { renderEffect = BlurEffect(10.dp.toPx(), 10.dp.toPx(), TileMode.Decal) }
        ) { drawFill(alpha = 0.55f, blendMode = BlendMode.SrcOver) }
        // the sharp fill, added on top of its glow
        Canvas(Modifier.matchParentSize()) { drawFill(alpha = 1f, blendMode = BlendMode.Plus) }
        // scale, labels, redline, needle and the number stay crisp and white over the glow
        Canvas(Modifier.matchParentSize()) { drawDial(speed, fill, hue, text) }
    }
}

private fun DrawScope.dialRadius() = min(size.width, size.height) * 0.42f

private fun DrawScope.pointAt(angleDeg: Float, r: Float): Offset {
    val a = Math.toRadians(angleDeg.toDouble())
    return Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
}

private fun angleOf(speed: Float) = StartDeg + SweepDeg * (speed / MaxSpeed).coerceIn(0f, 1f)

private fun hsv(hue: Float, s: Float = 0.9f, v: Float = 1f) =
    Color.hsv(wrapHue(hue), s, v)

private fun DrawScope.drawDial(speed: Float, fill: Float, hue: Float, text: TextMeasurer) {
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
    val red = hsv(hue + RedlineHueOffset)
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

// A recessed hub of dark concentric rings around the centre circle, with a short coloured
// segment along its lower arc (charge / range), a bolt and "210 km".
private fun DrawScope.drawHub(hue: Float, text: TextMeasurer) {
    val r = dialRadius()
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFF050506),
            0.55f to Color(0xFF0B0B0D),
            0.85f to Color(0xFF17181C),
            1f to Color(0xFF0A0A0B),
            center = center,
            radius = r * 0.56f,
        ),
        radius = r * 0.56f,
    )
    listOf(0.36f, 0.42f, 0.48f, 0.54f).forEachIndexed { i, k ->
        drawCircle(Color(0xFF2A2B31).copy(alpha = 0.9f - i * 0.15f), radius = r * k, style = Stroke(1.dp.toPx()))
    }
    // a faint light from above on the outer ring
    drawArc(
        Color.White.copy(alpha = 0.08f), 200f, 140f, false,
        Offset(center.x - r * 0.54f, center.y - r * 0.54f), Size(r * 1.08f, r * 1.08f),
        style = Stroke(3.dp.toPx()),
    )
    val ringR = r * 0.48f
    drawArc(
        hsv(hue), 65f, 50f, false,
        Offset(center.x - ringR, center.y - ringR), Size(2 * ringR, 2 * ringR),
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )
    // bolt
    val s = r * 0.035f
    val bolt = Path().apply {
        val o = center + Offset(0f, r * 0.37f)
        moveTo(o.x + 0.2f * s, o.y - s)
        lineTo(o.x - 0.6f * s, o.y + 0.1f * s)
        lineTo(o.x, o.y + 0.1f * s)
        lineTo(o.x - 0.2f * s, o.y + s)
        lineTo(o.x + 0.6f * s, o.y - 0.1f * s)
        lineTo(o.x, o.y - 0.1f * s)
        close()
    }
    drawPath(bolt, Color.White.copy(alpha = 0.8f))
    val range = text.measure("210 km", TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = (r * 0.05f / density / fontScale).sp))
    drawText(range, topLeft = center + Offset(-range.size.width / 2f, r * 0.6f))
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
