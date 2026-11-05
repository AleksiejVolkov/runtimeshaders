package com.offmind.runtimeshaders.screens.effects.speedometer

import androidx.compose.ui.graphics.Color

// Every speed on the scale has its color: a few stops, and straight lines between them.
private val Stops = listOf(
    0f to Color(0xFF00B8C4),
    120f to Color(0xFF2A3BE0),
    200f to Color(0xFFE01078),
    240f to Color(0xFFFF3020),
)

// The four stops, as the shader takes them: three numbers each.
fun stops() = FloatArray(12).also { out ->
    Stops.forEachIndexed { i, (_, color) ->
        out[i * 3] = color.red
        out[i * 3 + 1] = color.green
        out[i * 3 + 2] = color.blue
    }
}

// The same straight lines for the flat parts of the dial, drawn by Canvas.
fun colorAt(speed: Float): Color {
    val i = Stops.indexOfLast { it.first <= speed }.coerceIn(0, Stops.size - 2)
    val t = ((speed - Stops[i].first) / (Stops[i + 1].first - Stops[i].first)).coerceIn(0f, 1f)
    val a = Stops[i].second
    val b = Stops[i + 1].second
    return Color(
        a.red + (b.red - a.red) * t,
        a.green + (b.green - a.green) * t,
        a.blue + (b.blue - a.blue) * t,
    )
}
