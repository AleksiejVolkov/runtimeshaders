package com.offmind.runtimeshaders.screens.effects.speedometer

// A launch from 0 to 221 km/h, a short hold, then the foot comes off the gas and the car
// coasts back to 0: (time s, speed km/h). The way down is its own curve, not the launch
// reversed.
private val Keyframes = listOf(
    0.0f to 0f,
    0.4f to 2f,
    1.2f to 24f,
    2.0f to 70f,
    3.2f to 126f,
    4.0f to 170f,
    5.2f to 200f,
    5.5f to 221f,
    6.1f to 221f,
    6.7f to 196f,
    7.5f to 150f,
    8.4f to 96f,
    9.3f to 44f,
    10.0f to 12f,
    10.5f to 0f,
)
private const val PeakTime = 5.5f
// A moment of rest at 0: the spring settles, and the loop has no seam.
const val ScenarioLength = 11f
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

// The first moment the speed reaches [speed]: on the way up it only grows, so bisect.
fun timeOfSpeed(speed: Float): Float {
    var lo = 0f
    var hi = PeakTime
    repeat(40) {
        val mid = (lo + hi) / 2
        if (speedAt(mid) < speed) lo = mid else hi = mid
    }
    return hi
}

// What accumulates over time. The fill follows the speed on a critically damped spring, so it
// lags behind the number. The flow is a sum of rate * dt, not time * rate: the rate changes
// with speed, and a product would send the whole smoke rushing whenever the speed changes.
private const val Stiffness = 16f // rad/s: about 120 at 126 km/h and 165 at 170
private const val Step = 1f / 240f

class Launch {
    var fill = 0f
    var flow = 0f
    private var velocity = 0f
    private var left = 0f

    fun advance(target: Float, dt: Float) {
        left += dt
        while (left >= Step) {
            left -= Step
            val a = Stiffness * Stiffness * (target - fill) - 2f * Stiffness * velocity
            velocity += a * Step
            fill += velocity * Step
            flow += flowRate(fill) * Step
        }
    }

    // Where everything would be at [t] of the scenario: the same frame every time.
    fun seek(t: Float) {
        fill = 0f
        flow = 0f
        velocity = 0f
        left = 0f
        var time = 0f
        while (time < t) {
            advance(speedAt(time), Step)
            time += Step
        }
    }
}

// One number drives the smoke: the fill speed, as a share of the whole scale.
private fun share(speed: Float) = (speed / MaxSpeed).coerceIn(0f, 1f)

// How fast the smoke runs along the arc, in noise cells per second.
fun flowRate(speed: Float) = 0.25f + 3.2f * share(speed)

// How much the wisps are drawn out along the arc.
fun stretchAt(speed: Float) = 1f + 3f * share(speed) * share(speed)

// How far one noise pushes the coordinates of the other.
fun warpAt(speed: Float) = 1.2f + 1.6f * share(speed)

// How deep the inner edge is torn, in rims.
fun tearAt(speed: Float) = 0.1f + 0.14f * share(speed)

// How bright the fill is: a dim, see-through glow at low speed, overexposed only near the top.
// A square of the speed: slow to start, so 63 km/h stays dim.
fun exposureAt(speed: Float): Float {
    val s = (speed / 221f).coerceIn(0f, 1f)
    return 0.2f + 0.8f * s * s
}

// How far the dial has come apart past the redline: 0 at 200 km/h, 1 at 240, smoothstep so
// there is no kink where it starts.
fun breakdownAt(speed: Float): Float {
    val x = ((speed - 200f) / 40f).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
