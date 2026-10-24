package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RuntimeShader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ButtonWidth = 150.dp
private val ButtonHeight = 56.dp
private val ButtonShape = RoundedCornerShape(28.dp)

/**
 * The hook and chapter 12: two buttons that look the same. The left one starts as code an agent
 * really writes and takes the three fixes one at a time; the right one is always fully fixed.
 */
internal enum class GlowFix(val label: String, val note: String) {
    Agent(
        "Agent code",
        "new RuntimeShader every recomposition · time read in composition · ShaderBrush(shader) inline · chips get a new list",
    ),
    RememberShader(
        "1 · remember shader",
        "shader remembered · time still read in composition · new brush every frame",
    ),
    ReadInDraw(
        "2 · read in draw",
        "time read inside drawBehind · composition sleeps · brush still allocated in draw",
    ),
    Fixed(
        "3 · remember brush",
        "shader and brush remembered · time read in drawBehind only",
    ),
    // A trap, not a step: off the fix path, shown last.
    FrozenBrush(
        "✗ brush remembered, read in composition",
        "brush remembered but uniform still set in composition · nothing re-records · the glow freezes",
    ),
}

private enum class GlowLayout(val label: String) { Both("Both"), Left("Left only"), Right("Right only") }

@Composable
internal fun GlowButtonsScreen() {
    var fix by rememberSaveable { mutableStateOf(GlowFix.Agent) }
    var shown by rememberSaveable { mutableStateOf(GlowLayout.Both) }
    var readouts by rememberSaveable { mutableStateOf(true) }
    val left = remember { GlowCounters("left") }
    val right = remember { GlowCounters("right") }

    val variant = "left=${fix.name}/show=${shown.name}"
    RecordPhases("glow-buttons", variant, left.all + right.all)
    val frames by rememberFrameSummary("screen=glow-buttons variant=$variant")

    EngineScreen(
        title = "Two glow buttons",
        subtitle = "Same pixels. Watch which phases run every frame.",
    ) {
        EngineChoice("Left button", GlowFix.entries, fix, { it.label }) { fix = it }
        EngineChoice("Show", GlowLayout.entries, shown, { it.label }) { shown = it }
        EngineChoice("Readouts", listOf(true, false), readouts, { if (it) "On" else "Off" }) { readouts = it }
        FrameReadout(frames)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shown != GlowLayout.Right) {
                    VerdictTag(if (fix == GlowFix.Fixed) "Fixed" else fix.label, bad = fix != GlowFix.Fixed)
                    Text(fix.note, color = EngineDim, fontSize = 11.sp)
                    when (fix) {
                        GlowFix.Agent -> AgentGlowPanel(left)
                        GlowFix.RememberShader -> RememberedShaderGlowPanel(left)
                        GlowFix.ReadInDraw -> DrawReadGlowPanel(left)
                        GlowFix.Fixed -> FixedGlowPanel(left)
                        GlowFix.FrozenBrush -> FrozenBrushGlowPanel(left)
                    }
                    if (readouts) GlowReadouts(left)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shown != GlowLayout.Left) {
                    VerdictTag("Fixed", bad = false)
                    Text(GlowFix.Fixed.note, color = EngineDim, fontSize = 11.sp)
                    FixedGlowPanel(right)
                    if (readouts) GlowReadouts(right)
                }
            }
        }
    }
}

/** One panel = the button and its unchanged neighbours, so it shows what else wakes up with it. */
internal class GlowCounters(side: String) {
    val panel = PhaseCounter("$side-panel")
    val button = PhaseCounter("$side-button")
    val chips = PhaseCounter("$side-chips")
    val dot = PhaseCounter("$side-dot")
    val all = listOf(panel, button, chips, dot)
}

@Composable
private fun GlowReadouts(counters: GlowCounters) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("panel (title, button, dot, chips)", color = EngineDim, fontSize = 10.sp)
        PhaseReadout(counters.panel, showShaders = true)
        Text("button", color = EngineDim, fontSize = 10.sp)
        PhaseReadout(counters.button)
        Text("status dot (gets the shader)", color = EngineDim, fontSize = 10.sp)
        PhaseReadout(counters.dot)
        Text("chips (a List, equal content)", color = EngineDim, fontSize = 10.sp)
        PhaseReadout(counters.chips)
    }
}

// ---- The agent's version, as it is usually written: one composable, the animation read at the top.

@Composable
private fun AgentGlowPanel(counters: GlowCounters) {
    counters.panel.compositions++
    val t by rememberInfiniteTransition(label = "glow")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val density = LocalDensity.current
    val shader = newRuntimeShader(GLOW_SHADER, counters.panel) // new shader on every recomposition
    shader.setFloatUniform("time", t) // the animation is read in composition
    with(density) { shader.setFloatUniform("size", ButtonWidth.toPx(), ButtonHeight.toPx()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelTitle(shader, counters.dot)
        Box(
            modifier = Modifier
                .countLayout(counters.button)
                .width(ButtonWidth)
                .height(ButtonHeight)
                .clip(ButtonShape)
                .countDraw(counters.button)
                .background(ShaderBrush(shader)), // a new brush every time: no equals
            contentAlignment = Alignment.Center,
        ) { Text("Play", color = Color.White) }
        // A new list on every recomposition, yet the chips skip: List is an interface, so with
        // strong skipping the call compares it with equals, and the content is equal.
        DeviceChips(Devices.map { it.label }, counters.chips)
    }
}

@Composable
private fun RememberedShaderGlowPanel(counters: GlowCounters) {
    counters.panel.compositions++
    val t by rememberInfiniteTransition(label = "glow")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val density = LocalDensity.current
    val shader = remember { newRuntimeShader(GLOW_SHADER, counters.panel) }
    shader.setFloatUniform("time", t)
    with(density) { shader.setFloatUniform("size", ButtonWidth.toPx(), ButtonHeight.toPx()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelTitle(shader, counters.dot)
        Box(
            modifier = Modifier
                .countLayout(counters.button)
                .width(ButtonWidth)
                .height(ButtonHeight)
                .clip(ButtonShape)
                .countDraw(counters.button)
                .background(ShaderBrush(shader)),
            contentAlignment = Alignment.Center,
        ) { Text("Play", color = Color.White) }
        DeviceChips(Devices.map { it.label }, counters.chips)
    }
}

@Composable
private fun FrozenBrushGlowPanel(counters: GlowCounters) {
    counters.panel.compositions++
    val t by rememberInfiniteTransition(label = "glow")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val density = LocalDensity.current
    val shader = remember { newRuntimeShader(GLOW_SHADER, counters.panel) }
    val brush = remember(shader) { ShaderBrush(shader) }
    shader.setFloatUniform("time", t) // still in composition: the recorded draw keeps the old value
    with(density) { shader.setFloatUniform("size", ButtonWidth.toPx(), ButtonHeight.toPx()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelTitle(shader, counters.dot)
        Box(
            modifier = Modifier
                .countLayout(counters.button)
                .width(ButtonWidth)
                .height(ButtonHeight)
                .clip(ButtonShape)
                .countDraw(counters.button)
                .background(brush), // the same brush: the background node sees no change
            contentAlignment = Alignment.Center,
        ) { Text("Play", color = Color.White) }
        DeviceChips(Devices.map { it.label }, counters.chips)
    }
}

@Composable
private fun DrawReadGlowPanel(counters: GlowCounters) {
    counters.panel.compositions++
    val t = rememberInfiniteTransition(label = "glow")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val shader = remember { newRuntimeShader(GLOW_SHADER, counters.panel) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelTitle(shader, counters.dot)
        Box(
            modifier = Modifier
                .countLayout(counters.button)
                .width(ButtonWidth)
                .height(ButtonHeight)
                .clip(ButtonShape)
                .countDraw(counters.button)
                .drawBehind {
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("time", t.value) // read while drawing
                    drawRect(ShaderBrush(shader)) // still allocates in draw, every frame
                },
            contentAlignment = Alignment.Center,
        ) { Text("Play", color = Color.White) }
        DeviceChips(Devices.map { it.label }, counters.chips)
    }
}

internal data class Device(val label: String)

private val Devices = listOf(Device("Wi-Fi"), Device("Cast"), Device("Timer"))

@Composable
private fun FixedGlowPanel(counters: GlowCounters) {
    counters.panel.compositions++
    val t = rememberInfiniteTransition(label = "glow")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val shader = remember { newRuntimeShader(GLOW_SHADER, counters.panel) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val chips = remember { Devices.map { it.label } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelTitle(shader, counters.dot)
        Box(
            modifier = Modifier
                .countLayout(counters.button)
                .width(ButtonWidth)
                .height(ButtonHeight)
                .clip(ButtonShape)
                .countDraw(counters.button)
                .drawBehind {
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("time", t.value)
                    drawRect(brush)
                },
            contentAlignment = Alignment.Center,
        ) { Text("Play", color = Color.White) }
        DeviceChips(chips, counters.chips)
    }
}

/**
 * The panel title with a small status dot painted by the same glow shader. RuntimeShader has no
 * equals, so the dot recomposes whenever its caller hands it a new shader object.
 */
@Composable
private fun PanelTitle(shader: RuntimeShader, counter: PhaseCounter) {
    counter.compositions++
    val brush = remember(shader) { ShaderBrush(shader) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Living room", color = EngineText, fontSize = 15.sp)
        Box(
            Modifier
                .countLayout(counter)
                .size(10.dp)
                .clip(CircleShape)
                .countDraw(counter)
                .drawBehind { drawRect(brush) },
        )
    }
}

/** Neighbours that never change. They recompose only if their caller hands them a new list instance. */
@Composable
private fun DeviceChips(chips: List<String>, counter: PhaseCounter) {
    counter.compositions++
    Row(
        modifier = Modifier.countLayout(counter).countDraw(counter),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { chip ->
            Text(
                chip,
                color = EngineDim,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}
