package com.offmind.runtimeshaders.screens.effects.speedometer

import kotlin.math.min

// A launch from 0 to 221 km/h in 5.6 s: (time s, speed km/h).
private val Keyframes = listOf(
    0.0f to 0f,
    0.4f to 2f,
    1.2f to 24f,
    2.0f to 70f,
    3.2f to 126f,
    4.0f to 170f,
    5.2f to 200f,
    5.5f to 221f,
)
const val ScenarioLength = 5.6f
const val MaxSpeed = 240f

// Monotone cubic through the keyframes (Fritsch–Carlson): smooth, and never runs backwards.
private val slopes: FloatArray = run {
    val n = Keyframes.size
    val delta = FloatArray(n - 1) { i ->
        (Keyframes[i + 1].second - Keyframes[i].second) / (Keyframes[i + 1].first - Keyframes[i].first)
    }
    FloatArray(n) { i ->
        when (i) {
            0 -> delta[0]
            n - 1 -> delta[n - 2]
            else -> if (delta[i - 1] * delta[i] <= 0f) 0f else {
                val w1 = 2 * (Keyframes[i + 1].first - Keyframes[i].first) + (Keyframes[i].first - Keyframes[i - 1].first)
                val w2 = (Keyframes[i + 1].first - Keyframes[i].first) + 2 * (Keyframes[i].first - Keyframes[i - 1].first)
                (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i])
            }
        }
    }
}

// The speed the car really has: this is what the number and the needle show.
fun speedAt(t: Float): Float {
    if (t <= 0f) return 0f
    val last = Keyframes.last()
    if (t >= last.first) return last.second
    val i = Keyframes.indexOfLast { it.first <= t }
    val (t0, v0) = Keyframes[i]
    val (t1, v1) = Keyframes[i + 1]
    val h = t1 - t0
    val s = (t - t0) / h
    val s2 = s * s
    val s3 = s2 * s
    return (2 * s3 - 3 * s2 + 1) * v0 + (s3 - 2 * s2 + s) * h * slopes[i] +
        (-2 * s3 + 3 * s2) * v1 + (s3 - s2) * h * slopes[i + 1]
}

// The fill follows the speed on a critically damped spring, so it lags behind the number.
// It is integrated from the start every time, which makes any moment reproducible.
private const val Stiffness = 16f // rad/s: about 120 at 126 km/h and 165 at 170
private const val Step = 1f / 240f

fun fillSpeedAt(t: Float): Float {
    var x = 0f
    var v = 0f
    var time = 0f
    while (time < t) {
        val dt = min(Step, t - time)
        val target = speedAt(time)
        val a = Stiffness * Stiffness * (target - x) - 2f * Stiffness * v
        v += a * dt
        x += v * dt
        time += dt
    }
    return x
}

// The first moment the speed reaches [speed]: the scenario only goes up, so bisect.
fun timeOfSpeed(speed: Float): Float {
    var lo = 0f
    var hi = Keyframes.last().first
    repeat(40) {
        val mid = (lo + hi) / 2
        if (speedAt(mid) < speed) lo = mid else hi = mid
    }
    return hi
}

// Hue in degrees. It keeps turning at 350°/s towards smaller values (blue, cyan, green...),
// starting where the redline (fill + 150°) is red: red is 0° in HSV and about 29° in OKLCH.
const val HueSpeed = 350f
const val RedlineHueOffset = 150f

fun hueStart(oklch: Boolean) = if (oklch) 29f - RedlineHueOffset else 360f - RedlineHueOffset

fun cycleHueAt(t: Float, oklch: Boolean): Float = wrapHue(hueStart(oklch) - HueSpeed * t)

fun speedHue(speed: Float, oklch: Boolean): Float = wrapHue(hueStart(oklch) - speed * 1.5f)

fun wrapHue(h: Float): Float = ((h % 360f) + 360f) % 360f

// How bright the fill is: a dim, see-through glow at low speed, overexposed only near the top.
// A square of the (smoothed) speed: slow to start, so 63 km/h stays dim.
fun exposureAt(fillSpeed: Float): Float {
    val s = (fillSpeed / 221f).coerceIn(0f, 1f)
    return 0.2f + 0.8f * s * s
}

// Chromatic aberration past the redline: 0 at 200 km/h, 1 at 240, smoothstep so there is no
// kink where it starts.
fun aberrationAt(speed: Float): Float {
    val x = ((speed - 200f) / 40f).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
