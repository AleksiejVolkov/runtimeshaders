package com.offmind.runtimeshaders.screens.effects.glassrefraction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

// A piece of thick glass over a live list. Everything the glass bends is the list below it.
@Composable
fun GlassRefractionScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF101114))) {
        val card = remember { GlassPanel() }
        val toolbar = remember { GlassPanel() }
        val button = remember { GlassPanel() }
        val player = remember { GlassPanel() }
        val panels = listOf(card, toolbar, button, player)
        var frosted by remember { mutableStateOf(false) }
        var mode by remember { mutableStateOf(GlassMode.Backdrop) }
        val useHaze = mode == GlassMode.Haze
        val hazeState = rememberHazeState()
        // the same screen three ways: our glass on the backdrop, Haze's glass on every panel,
        // or the platform's backdrop effect on every panel
        val panelGlass = when (mode) {
            GlassMode.Backdrop -> null
            GlassMode.Haze -> PanelGlass.Haze(hazeState, frosted)
            GlassMode.Platform -> PanelGlass.Platform(frosted)
        }
        // Haze records the list only while the list draws, and a list that stands still does
        // not draw again: without one more frame the new glass would have nothing to show.
        var redraws by remember { mutableIntStateOf(0) }
        LaunchedEffect(useHaze) {
            withFrameNanos { }
            redraws++
        }
        CardList(
            when (mode) {
                GlassMode.Backdrop -> Modifier.fillMaxSize().glassBackdrop(panels, frosted)
                GlassMode.Haze -> Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .drawBehind { redraws }
                GlassMode.Platform -> Modifier.fillMaxSize()
            }
        )

        GlassBox(
            card, 36.dp, panelGlass,
            Modifier.align(Alignment.Center).size(width = 320.dp, height = 220.dp),
        )
        GlassBox(
            toolbar, 36.dp, panelGlass,
            Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth().height(72.dp),
        ) {
            Text("Cards", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        GlassBox(
            button, 48.dp, panelGlass,
            Modifier.align(Alignment.TopEnd).padding(top = 190.dp, end = 24.dp).size(96.dp),
        )
        GlassBox(
            player, 28.dp, panelGlass,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
                .fillMaxWidth()
                .height(80.dp),
        ) {
            Text("Now playing · Glass", color = Color.White, fontSize = 18.sp)
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Chip(text = if (frosted) "frosted" else "clear", onClick = { frosted = !frosted })
            Chip(text = mode.label, onClick = { mode = mode.next() })
        }
    }
}

private enum class GlassMode(val label: String) {
    Backdrop("backdrop"), Haze("Haze"), Platform("API 37.2");

    fun next(): GlassMode {
        val modes = entries.filter { it != Platform || PlatformBackdropAvailable }
        return modes[(modes.indexOf(this) + 1) % modes.size]
    }
}

// Glass drawn by the panel itself, not by the backdrop.
private sealed interface PanelGlass {
    class Haze(val state: HazeState, val frosted: Boolean) : PanelGlass
    class Platform(val frosted: Boolean) : PanelGlass
}

// The panel draws only its outline and its own content; the glass under it is drawn by the
// backdrop. With Haze and on Android 37.2 it is the other way round: the panel draws the
// glass.
@Composable
private fun GlassBox(
    panel: GlassPanel,
    cornerRadius: Dp,
    panelGlass: PanelGlass?,
    modifier: Modifier,
    content: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glass = when (panelGlass) {
        is PanelGlass.Haze -> Modifier.hazeGlass(
            input = HazeInput.Sources(panelGlass.state),
            style = GlassStyle {
                shape(shape)
                optics(blurRadius = if (panelGlass.frosted) FrostRadius else 0.dp)
            },
        ).clip(shape)
        is PanelGlass.Platform -> Modifier.platformGlass(cornerRadius, panelGlass.frosted)
        null -> Modifier
    }
    Box(
        modifier
            .glassPanel(panel, cornerRadius)
            .then(glass)
            .border(1.dp, Color.White.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center,
    ) {
        content()
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
