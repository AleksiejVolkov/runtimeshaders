package com.offmind.runtimeshaders.screens.effects.neumorphism

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            NeuSample("SQUARE") {
                Box(Modifier.size(110.dp).neumorphic(cornerRadius = 22.dp))
            }
            NeuSample("CIRCLE") {
                Box(Modifier.size(110.dp).neumorphic(cornerRadius = 55.dp))
            }
        }
    }
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
