package com.offmind.runtimeshaders.screens.effects.speedometer

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

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

private fun cbrt(x: Float) = Math.cbrt(x.toDouble()).toFloat()

private fun linearToOkLab(c: FloatArray): FloatArray {
    val l = cbrt(0.4122214708f * c[0] + 0.5363325363f * c[1] + 0.0514459929f * c[2])
    val m = cbrt(0.2119034982f * c[0] + 0.6806995451f * c[1] + 0.1073969566f * c[2])
    val s = cbrt(0.0883024619f * c[0] + 0.2817188376f * c[1] + 0.6299787005f * c[2])
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

private fun okLabToLinear(lab: FloatArray): FloatArray {
    val l = lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2]
    val m = lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2]
    val s = lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2]
    val l3 = l * l * l
    val m3 = m * m * m
    val s3 = s * s * s
    return floatArrayOf(
        (4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3).coerceAtLeast(0f),
        (-1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3).coerceAtLeast(0f),
        (-0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3).coerceAtLeast(0f),
    )
}

// OKLCH: lightness, chroma and hue go separately, so the middle stays as saturated as the
// ends.
// The hue is an angle, and an angle can go two ways round: take the shorter one. From 200 to
// 240 km/h the plain difference is -330°, which would run through the whole circle of hues.
private fun mixLch(a: FloatArray, b: FloatArray, t: Float): FloatArray {
    val ca = hypot(a[1], a[2])
    val cb = hypot(b[1], b[2])
    val ha = atan2(a[2], a[1])
    var dh = atan2(b[2], b[1]) - ha
    if (dh > PI.toFloat()) dh -= 2 * PI.toFloat()
    if (dh < -PI.toFloat()) dh += 2 * PI.toFloat()
    val c = ca + (cb - ca) * t
    val h = ha + dh * t
    return floatArrayOf(a[0] + (b[0] - a[0]) * t, c * cos(h), c * sin(h))
}

// The color of a speed, in linear light: that is what the shader works with.
fun colorAt(speed: Float): FloatArray {
    val i = Stops.indexOfLast { it.first <= speed }.coerceIn(0, Stops.size - 2)
    val t = ((speed - Stops[i].first) / (Stops[i + 1].first - Stops[i].first)).coerceIn(0f, 1f)
    val a = channels(Stops[i].second)
    val b = channels(Stops[i + 1].second)
    val labA = linearToOkLab(FloatArray(3) { decode(a[it]) })
    val labB = linearToOkLab(FloatArray(3) { decode(b[it]) })
    return okLabToLinear(mixLch(labA, labB, t))
}

// The same color for the flat parts of the dial, drawn by Canvas.
fun FloatArray.toEncodedColor() = Color(encode(this[0]), encode(this[1]), encode(this[2]))

// The scale sampled every 10 km/h: the shader mixes two neighbours.
const val RampSize = 25

fun ramp() = FloatArray(RampSize * 3).also { out ->
    for (i in 0 until RampSize) colorAt(i * 10f).copyInto(out, i * 3)
}
