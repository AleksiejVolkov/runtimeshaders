package com.offmind.runtimeshaders.screens.effects.composeengine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val EngineBackground = Color(0xFF0B0D14)
internal val EngineText = Color(0xFFE6E9F2)
internal val EngineDim = Color(0xFF8A90A3)
internal val EngineBad = Color(0xFFFF6B6B)
internal val EngineGood = Color(0xFF6BE3A4)
internal val EngineAccent = Color(0xFF7FA2FF)

internal val MonoSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = EngineText)

/** Dark demo page: title, one-line purpose, the switches, then the demo. */
@Composable
internal fun EngineScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    background: Color = EngineBackground,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = EngineText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = EngineDim, fontSize = 13.sp)
        content()
    }
}

/** A row of mutually exclusive options; switches between the variants of a demo. */
@Composable
internal fun <T> EngineChoice(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = EngineDim, fontSize = 10.sp, letterSpacing = 1.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) EngineAccent.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (isSelected) EngineAccent else EngineDim.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(optionLabel(option), color = if (isSelected) EngineText else EngineDim, fontSize = 12.sp)
                }
            }
        }
    }
}

/** "AGENT CODE" / "FIXED" label next to a variant, so a bad demo is never mistaken for advice. */
@Composable
internal fun VerdictTag(text: String, bad: Boolean, modifier: Modifier = Modifier) {
    val color = if (bad) EngineBad else EngineGood
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Per-second phase rates of one element. Reads only [PhaseCounter.perSecond], written once a second. */
@Composable
internal fun PhaseReadout(counter: PhaseCounter, modifier: Modifier = Modifier, showShaders: Boolean = false) {
    val r = counter.perSecond
    val lines = buildList {
        add("composition  ${r.compositions}/s")
        add("measure      ${r.measures}/s   place ${r.placements}/s")
        if (r.placementReads > 0) add("offset { }   ${r.placementReads}/s")
        if (r.layerReads > 0) add("layer { }    ${r.layerReads}/s")
        add("draw         ${r.draws}/s")
        if (showShaders) add("new shader   ${r.shaders}/s")
    }
    Text(
        text = lines.joinToString("\n"),
        style = MonoSmall,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(6.dp),
    )
}

/** One line of frame timings from the platform's FrameMetrics. */
@Composable
internal fun FrameReadout(summary: FrameSummary?, modifier: Modifier = Modifier) {
    Text(
        text = summary?.toDisplay() ?: "measuring frames…",
        style = MonoSmall.copy(color = EngineAccent),
        modifier = modifier.fillMaxWidth(),
    )
}
