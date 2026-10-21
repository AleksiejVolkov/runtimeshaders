package com.offmind.runtimeshaders.screens.effects.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// What lies behind the glass: a calm gradient and a few big soft spots of light that drift slowly.
// Nothing here is a shader, it's just something for the glass to be transparent over.
data class GlassMood(
    val top: Color,
    val bottom: Color,
    val spots: List<Color>,
)

val TealMood = GlassMood(
    top = Color(0xFF8FB3BD),
    bottom = Color(0xFF3E5E68),
    spots = listOf(Color(0x66E6F4F7), Color(0x55244A57), Color(0x449FD3DC)),
)

val DayMood = GlassMood(
    top = Color(0xFFE7F1F8),
    bottom = Color(0xFFC4D9E6),
    spots = listOf(Color(0x88FFFFFF), Color(0x18244A57), Color(0x309FD3DC)),
)

val NightMood = GlassMood(
    top = Color(0xFF3B3E48),
    bottom = Color(0xFF121317),
    spots = listOf(Color(0x55C9CEDC), Color(0x44000000), Color(0x336B7488)),
)

fun lerp(a: GlassMood, b: GlassMood, t: Float) = GlassMood(
    top = lerp(a.top, b.top, t),
    bottom = lerp(a.bottom, b.bottom, t),
    spots = a.spots.zip(b.spots) { x, y -> lerp(x, y, t) },
)

@Composable
fun GlassBackdrop(
    mood: GlassMood,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing)),
        label = "phase"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(mood.top, mood.bottom)))
                mood.spots.forEachIndexed { i, color ->
                    val a = 2f * PI.toFloat() * (drift + i / 3f)
                    val center = Offset(
                        size.width * (0.5f + 0.38f * cos(a + i)),
                        size.height * (0.3f + 0.22f * i + 0.08f * sin(a * 2f))
                    )
                    val radius = size.width * (0.55f + 0.1f * i)
                    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
                }
            },
        content = content
    )
}
