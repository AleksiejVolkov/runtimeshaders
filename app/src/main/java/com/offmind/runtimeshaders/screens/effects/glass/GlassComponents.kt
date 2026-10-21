package com.offmind.runtimeshaders.screens.effects.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GlassInk = Color(0xF2FFFFFF)
val GlassInkSoft = Color(0x99FFFFFF)

// A mood of the glass: only colors. The rules (edge, Fresnel, two highlights, shadow) never change
@Immutable
data class GlassStyle(val tint: Color, val tintAlpha: Float, val shine: Color, val wellAlpha: Float)

val DayGlass = GlassStyle(Color.White, tintAlpha = 0.10f, shine = Color.White, wellAlpha = 0.16f)
// at night the body is barely there and the light is a little colder
val NightGlass = GlassStyle(
    Color.White, tintAlpha = 0.05f, shine = Color(0xFFDCE4FF), wellAlpha = 0.32f
)

fun lerp(a: GlassStyle, b: GlassStyle, t: Float) = GlassStyle(
    tint = lerp(a.tint, b.tint, t),
    tintAlpha = lerp(a.tintAlpha, b.tintAlpha, t),
    shine = lerp(a.shine, b.shine, t),
    wellAlpha = lerp(a.wellAlpha, b.wellAlpha, t),
)

val LocalGlassStyle = staticCompositionLocalOf { DayGlass }

// Modifier.glass with the colors of the current mood
fun Modifier.styledGlass(
    cornerRadius: Dp,
    thickness: Dp = 12.dp,
    lift: Dp = 10.dp,
    depth: Float = 1f,
) = composed {
    val style = LocalGlassStyle.current
    glass(
        cornerRadius, style.tint, style.tintAlpha,
        thickness = thickness, shine = style.shine, lift = lift, depth = depth
    )
}

// Everything below is the same modifier with different parameters. Three roles are enough:
// a raised piece of glass, a pressed one, and a well cut into the glass for "selected".
@Composable
fun GlassPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // pressed: the glass comes down to the surface, so the edge catches less light
    // and the shadow tucks in under it
    val depth by animateFloatAsState(if (pressed) 0.35f else 1f, tween(120), label = "depth")
    val lift by animateDpAsState(if (pressed) 3.dp else 10.dp, tween(120), label = "lift")
    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = 1f - (1f - depth) * 0.04f
                scaleX = scale
                scaleY = scale
            }
            .styledGlass(cornerRadius, lift = lift, depth = depth)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// a well: the same glass turned inside out, darker, without a shadow of its own
fun Modifier.glassWell(cornerRadius: Dp) = composed {
    val style = LocalGlassStyle.current
    glass(
        cornerRadius, tint = Color.Black, tintAlpha = style.wellAlpha,
        shine = style.shine, lift = 0.dp, depth = -1f
    )
}

@Composable
fun GlassTabBar(
    tabs: List<Pair<String, ImageVector>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(76.dp)
            .styledGlass(cornerRadius = 38.dp)
            .padding(6.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val x by animateDpAsState(
            tabWidth * selected,
            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "tab"
        )
        // the well slides under the selected tab
        Box(
            Modifier
                .offset(x = x)
                .width(tabWidth)
                .fillMaxHeight()
                .glassWell(cornerRadius = 32.dp)
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { i, (label, icon) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val color = if (i == selected) GlassInk else GlassInkSoft
                    Icon(icon, null, Modifier.size(22.dp), tint = color)
                    Text(label, fontSize = 12.sp, color = color, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun GlassSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(64.dp)
            .styledGlass(cornerRadius = 32.dp, thickness = 22.dp)
            .padding(7.dp)
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(
            w * selected,
            spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "segment"
        )
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .glassWell(cornerRadius = 25.dp)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 18.sp, color = if (i == selected) GlassInk else GlassInkSoft)
                }
            }
        }
    }
}

@Composable
fun GlassSearchField(hint: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .styledGlass(cornerRadius = 28.dp, thickness = 8.dp, lift = 6.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = GlassInkSoft)
        Text(hint, fontSize = 16.sp, color = GlassInkSoft, modifier = Modifier.padding(start = 12.dp))
    }
}

// a switch: the track is a well, the knob is a small raised piece of glass sliding in it
@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val t by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "switch"
    )
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 40.dp)
            .glassWell(cornerRadius = 20.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                onCheckedChange(!checked)
            }
            .padding(5.dp)
    ) {
        Box(
            Modifier
                .offset(x = 32.dp * t)
                .size(30.dp)
                .styledGlass(cornerRadius = 15.dp, thickness = 6.dp, lift = 4.dp)
        )
    }
}

@Composable
fun GlassLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = GlassInk)
}

@Composable
fun GlassRoundButton(icon: ImageVector, onClick: () -> Unit, size: Dp = 64.dp) {
    GlassPressable(onClick = onClick, modifier = Modifier.size(size), cornerRadius = size / 2) {
        Icon(icon, null, Modifier.size(size * 0.42f), tint = GlassInk)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .styledGlass(cornerRadius = 28.dp, thickness = 16.dp)
            .padding(22.dp)
    ) {
        content()
    }
}
