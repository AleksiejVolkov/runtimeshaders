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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun SpeedometerScreen() {
    var time by remember { mutableFloatStateOf(0f) } // scenario time
    var playing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1e9f
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
    val fillSpeed = speed
    val hue = cycleHueAt(time)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Speedometer(
            speed = speed,
            fill = fillSpeed / MaxSpeed,
            hue = hue,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().aspectRatio(1f),
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(if (playing) "pause" else "play") { playing = !playing }
            listOf(63, 112, 170, 215, 221).forEach { target ->
                Chip("$target") {
                    playing = false
                    time = timeOfSpeed(target.toFloat())
                }
            }
        }
    }
}

@Composable
private fun Speedometer(
    speed: Float,
    fill: Float,
    hue: Float,
    modifier: Modifier,
) {
    val shader = remember { RuntimeShader(speedFillShader) }
    val brush = remember { ShaderBrush(shader) }
    val text = rememberTextMeasurer()
    val tint = Color.hsv(hue, 0.9f, 1f)
    val redlineTint = Color.hsv(wrapHue(hue + RedlineHueOffset), 0.9f, 1f)

    // feeds the fill shader
    fun DrawScope.drawFill() {
        shader.setFloatUniform("center", center.x, center.y)
        shader.setFloatUniform("radius", dialRadius())
        shader.setFloatUniform("inner", InnerBand)
        shader.setFloatUniform("startAngle", Math.toRadians(StartDeg.toDouble()).toFloat())
        shader.setFloatUniform("sweep", Math.toRadians(SweepDeg.toDouble()).toFloat())
        shader.setFloatUniform("fill", fill)
        shader.setFloatUniform("hue", hue / 360f)
        drawRect(brush)
    }

    Box(modifier) {
        // the hub sits under everything
        Canvas(Modifier.matchParentSize()) { drawHub(tint, text) }
        // the fill
        Canvas(Modifier.matchParentSize()) { drawFill() }
        // scale, labels, redline, needle and the number stay crisp and white over the fill
        Canvas(Modifier.matchParentSize()) { drawDial(speed, fill, redlineTint, text) }
    }
}

private fun DrawScope.dialRadius() = min(size.width, size.height) * 0.42f

private fun DrawScope.pointAt(angleDeg: Float, r: Float): Offset {
    val a = Math.toRadians(angleDeg.toDouble())
    return Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
}

private fun angleOf(speed: Float) = StartDeg + SweepDeg * (speed / MaxSpeed).coerceIn(0f, 1f)

private fun DrawScope.arc(color: Color, r: Float, from: Float, sweep: Float, width: Float) =
    drawArc(
        color, from, sweep, false,
        Offset(center.x - r, center.y - r), Size(2 * r, 2 * r),
        style = Stroke(width, cap = StrokeCap.Butt),
    )

private fun DrawScope.label(text: TextMeasurer, value: String, at: Offset, style: TextStyle) {
    val layout = text.measure(value, style)
    drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

private fun DrawScope.textSize(r: Float, k: Float) = (r * k / density / fontScale).sp

private fun DrawScope.drawDial(speed: Float, fill: Float, redline: Color, text: TextMeasurer) {
    val r = dialRadius()

    // the rim, brighter over the filled part
    arc(Color.White.copy(alpha = 0.35f), r, StartDeg, SweepDeg, 1.5.dp.toPx())
    arc(Color.White, r, StartDeg, SweepDeg * fill, 1.5.dp.toPx())

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
    val labelStyle = TextStyle(Color.White, textSize(r, 0.085f), FontWeight.Medium)
    for (value in 0..240 step 20) {
        label(text, value.toString(), pointAt(angleOf(value.toFloat()), r * 0.8f), labelStyle)
    }

    // redline outside the scale, in the complementary colour, with a faint glow
    val redStart = angleOf(RedlineFrom)
    val redSweep = angleOf(MaxSpeed) - redStart
    arc(redline.copy(alpha = 0.18f), r * 1.07f, redStart, redSweep, 10.dp.toPx())
    arc(redline.copy(alpha = 0.45f), r * 1.07f, redStart, redSweep, 4.dp.toPx())
    arc(redline, r * 1.07f, redStart, redSweep, 2.dp.toPx())

    // needle: a thin line from the centre circle to the rim, at the real speed
    val needle = angleOf(speed)
    val width = 1.5.dp.toPx()
    drawLine(Color.White, pointAt(needle, r * 0.3f), pointAt(needle, r * 0.985f), width)

    // centre: a thin circle, the number and the unit
    drawCircle(Color.White.copy(alpha = 0.85f), radius = r * 0.3f, style = Stroke(width))
    val numberStyle = TextStyle(Color.White, textSize(r, 0.2f), FontWeight.Medium)
    label(text, speed.roundToInt().toString(), center - Offset(0f, r * 0.04f), numberStyle)
    val unitStyle = TextStyle(Color.White.copy(alpha = 0.8f), textSize(r, 0.065f))
    label(text, "km/h", center + Offset(0f, r * 0.14f), unitStyle)
}

// A recessed hub of dark concentric rings around the centre circle, with a short coloured
// segment along its lower arc (charge / range), a bolt and "210 km".
private fun DrawScope.drawHub(tint: Color, text: TextMeasurer) {
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
        val ring = Color(0xFF2A2B31).copy(alpha = 0.9f - i * 0.15f)
        drawCircle(ring, radius = r * k, style = Stroke(1.dp.toPx()))
    }
    // a faint light from above on the outer ring
    arc(Color.White.copy(alpha = 0.08f), r * 0.54f, 200f, 140f, 3.dp.toPx())
    drawArc(
        tint, 65f, 50f, false,
        Offset(center.x - r * 0.48f, center.y - r * 0.48f), Size(r * 0.96f, r * 0.96f),
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )
    // bolt
    val s = r * 0.035f
    val o = center + Offset(0f, r * 0.37f)
    val bolt = Path().apply {
        moveTo(o.x + 0.2f * s, o.y - s)
        lineTo(o.x - 0.6f * s, o.y + 0.1f * s)
        lineTo(o.x, o.y + 0.1f * s)
        lineTo(o.x - 0.2f * s, o.y + s)
        lineTo(o.x + 0.6f * s, o.y - 0.1f * s)
        lineTo(o.x, o.y - 0.1f * s)
        close()
    }
    drawPath(bolt, Color.White.copy(alpha = 0.8f))
    val rangeStyle = TextStyle(Color.White.copy(alpha = 0.6f), textSize(r, 0.05f))
    label(text, "210 km", center + Offset(0f, r * 0.63f), rangeStyle)
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
