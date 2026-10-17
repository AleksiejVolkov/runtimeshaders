package com.offmind.runtimeshaders.screens.effects.neumorphism

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeuColors.Base),
        content = content
    )
}

@Composable
fun NeumorphismBasicsScreen() {
    NeuSurface {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(56.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                NeuSample("FLAT") { NeuShape(cornerRadius = 22.dp) }
                NeuSample("CONCAVE") { NeuShape(cornerRadius = 55.dp, curve = -0.075f) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                NeuSample("CONCAVE") { NeuShape(cornerRadius = 22.dp, curve = -0.075f) }
                NeuSample("PRESSED") { NeuShape(cornerRadius = 22.dp, pressedByDefault = true) }
            }
        }
    }
}

// a shape you can press: the press state goes straight into the shader
@Composable
private fun NeuShape(cornerRadius: Dp, curve: Float = 0f, pressedByDefault: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (isPressed != pressedByDefault) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "press"
    )
    Box(
        Modifier
            .size(110.dp)
            .neumorphic(cornerRadius, curve = curve, press = press)
            .clickable(interaction, indication = null) {}
    )
}

@Composable
private fun NeuSample(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
        Spacer(Modifier.height(28.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeuColors.Hint
        )
    }
}
