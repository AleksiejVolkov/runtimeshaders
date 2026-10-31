package com.offmind.runtimeshaders.screens.effects.speedometer

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

// Monotone cubic through the keyframes (Fritsch-Carlson): smooth, and never runs backwards.
private val slopes: FloatArray = run {
    val n = Keyframes.size
    val t = FloatArray(n) { Keyframes[it].first }
    val v = FloatArray(n) { Keyframes[it].second }
    val delta = FloatArray(n - 1) { i -> (v[i + 1] - v[i]) / (t[i + 1] - t[i]) }
    FloatArray(n) { i ->
        when (i) {
            0 -> delta[0]
            n - 1 -> delta[n - 2]
            else -> if (delta[i - 1] * delta[i] <= 0f) 0f else {
                val w1 = 2 * (t[i + 1] - t[i]) + (t[i] - t[i - 1])
                val w2 = (t[i + 1] - t[i]) + 2 * (t[i] - t[i - 1])
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
// starting where the redline (fill + 150°) is red.
const val HueSpeed = 350f
const val RedlineHueOffset = 150f

fun cycleHueAt(t: Float): Float = wrapHue(360f - RedlineHueOffset - HueSpeed * t)

fun wrapHue(h: Float): Float = ((h % 360f) + 360f) % 360f
