package com.offmind.runtimeshaders.screens.effects.neumorphism

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// A round concave button that sinks into the surface while it's pressed
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "press"
    )
    Box(
        modifier = modifier
            .size(size)
            .neumorphic(cornerRadius = size / 2, curve = -0.075f, press = press)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content(isPressed)
    }
}

// The track is pressed into the surface, the knob is a small concave dish on top of it
@Composable
fun NeuToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val t by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "toggle"
    )
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 34.dp)
            .neumorphic(cornerRadius = 17.dp, press = 1f, distance = 4.dp, blur = 8.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                onCheckedChange(!checked)
            }
            .padding(4.dp)
    ) {
        // a tiny status LED, like on a real wall switch: the only accent on the screen
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 9.dp)
                .size(8.dp)
                .background(lerp(NeuColors.Base, NeuColors.Accent, t), CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(x = 30.dp * t)
                .size(26.dp)
                .neumorphic(cornerRadius = 13.dp, curve = -0.075f, distance = 3.dp, blur = 6.dp)
        )
    }
}
