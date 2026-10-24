package com.offmind.runtimeshaders.screens.effects.glassrefraction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A piece of thick glass over a live list. Everything the glass bends is the list below it.
@Composable
fun GlassRefractionScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF101114))) {
        val panel = remember { GlassPanel() }
        var frosted by remember { mutableStateOf(false) }
        CardList(Modifier.fillMaxSize().glassBackdrop(panel, frosted))

        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = 320.dp, height = 220.dp)
                .glassPanel(panel, cornerRadius = 36.dp)
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(36.dp))
        )

        Chip(
            text = if (frosted) "frosted" else "clear",
            onClick = { frosted = !frosted },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC101114))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}

private val CardColors = listOf(
    Color(0xFFFF5252) to Color(0xFFFFB74D),
    Color(0xFF7C4DFF) to Color(0xFF40C4FF),
    Color(0xFF00C853) to Color(0xFFC6FF00),
    Color(0xFFFF4081) to Color(0xFFEA80FC),
    Color(0xFFFFD600) to Color(0xFFFF6D00),
    Color(0xFF00B8D4) to Color(0xFF1DE9B6),
)

// Bright cards with straight lines on them: straight lines show bending best.
@Composable
private fun CardList(modifier: Modifier) {
    LazyColumn(
        // the page color is part of the list: glass bends only what is in the backdrop's
        // layer, and a gap between cards would otherwise come through it as plain black
        modifier = modifier.background(Color(0xFF101114)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(200) { index ->
            val (from, to) = CardColors[index % CardColors.size]
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(from, to)))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Card ${index + 1}",
                    color = Color(0xFF101114),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                listOf(1f, 0.8f, 0.55f).forEach { width ->
                    Box(
                        Modifier
                            .fillMaxWidth(width)
                            .height(6.dp)
                            .background(
                                Color(0xFF101114).copy(alpha = 0.75f),
                                RoundedCornerShape(3.dp),
                            )
                    )
                }
            }
        }
    }
}
