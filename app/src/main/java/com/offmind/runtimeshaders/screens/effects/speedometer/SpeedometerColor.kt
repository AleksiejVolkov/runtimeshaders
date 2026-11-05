package com.offmind.runtimeshaders.screens.effects.speedometer

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

// Every speed on the scale has its color: a few stops, and what lies between them.
private val Stops = listOf(
    0f to 0xFF00B8C4,
    120f to 0xFF2A3BE0,
    200f to 0xFFE01078,
    240f to 0xFFFF3020,
)

private fun decode(c: Float) =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

private fun encode(c: Float) = c.coerceIn(0f, 1f).let {
    if (it <= 0.0031308f) it * 12.92f else 1.055f * it.pow(1f / 2.4f) - 0.055f
}

private fun channels(argb: Long) = floatArrayOf(
    ((argb shr 16) and 0xFF) / 255f,
    ((argb shr 8) and 0xFF) / 255f,
    (argb and 0xFF) / 255f,
)

// The color of a speed, in linear light: that is what the shader works with.
fun colorAt(speed: Float): FloatArray {
    val i = Stops.indexOfLast { it.first <= speed }.coerceIn(0, Stops.size - 2)
    val t = ((speed - Stops[i].first) / (Stops[i + 1].first - Stops[i].first)).coerceIn(0f, 1f)
    val a = channels(Stops[i].second)
    val b = channels(Stops[i + 1].second)
    return FloatArray(3) { decode(a[it] + (b[it] - a[it]) * t) }
}

// The same color for the flat parts of the dial, drawn by Canvas.
fun FloatArray.toEncodedColor() = Color(encode(this[0]), encode(this[1]), encode(this[2]))

// The scale sampled every 10 km/h: the shader mixes two neighbours.
const val RampSize = 25

fun ramp() = FloatArray(RampSize * 3).also { out ->
    for (i in 0 until RampSize) colorAt(i * 10f).copyInto(out, i * 3)
}
