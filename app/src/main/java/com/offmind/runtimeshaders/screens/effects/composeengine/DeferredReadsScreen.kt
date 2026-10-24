package com.offmind.runtimeshaders.screens.effects.composeengine

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Travel = 220f // dp
private val SquareSize = 56.dp

/** Chapter 6: one animation, read in three different phases. */
internal enum class ReadPhase(val label: String, val code: String, val bad: Boolean) {
    Composition("Composition", "Modifier.offset(x.value.dp)", bad = true),
    Placement("Placement", "Modifier.offset { IntOffset(x.value.dp.roundToPx(), 0) }", bad = false),
    Layer("Layer", "Modifier.graphicsLayer { translationX = x.value.dp.toPx() }", bad = false),
}

private enum class ReadShown(val label: String, val phase: ReadPhase?) {
    All("All three", null),
    Composition("Composition", ReadPhase.Composition),
    Placement("Placement", ReadPhase.Placement),
    Layer("Layer", ReadPhase.Layer),
}

@Composable
internal fun DeferredReadsScreen() {
    var shown by rememberSaveable { mutableStateOf(ReadShown.All) }
    var readouts by rememberSaveable { mutableStateOf(true) }
    val counters = remember { ReadPhase.entries.associateWith { PhaseCounter(it.name.lowercase()) } }

    // The screen holds the State and never reads its value: each square decides where it reads it.
    val x = rememberInfiniteTransition(label = "move").animateFloat(
        initialValue = 0f,
        targetValue = Travel,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "x",
    )

    RecordPhases("deferred-reads", shown.name, counters.values.toList())
    val frames by rememberFrameSummary("screen=deferred-reads variant=${shown.name}")

    EngineScreen(
        title = "Where you read the value",
        subtitle = "The same animation, the same pixels. Only the phase that reads x differs.",
    ) {
        EngineChoice("Show", ReadShown.entries, shown, { it.label }) { shown = it }
        EngineChoice("Readouts", listOf(true, false), readouts, { if (it) "On" else "Off" }) { readouts = it }
        FrameReadout(frames)

        ReadPhase.entries.filter { shown.phase == null || shown.phase == it }.forEach { phase ->
            val counter = counters.getValue(phase)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerdictTag(if (phase.bad) "Read in composition" else "Deferred read", bad = phase.bad)
                    Text(phase.label, color = EngineText, fontSize = 13.sp)
                }
                Text(phase.code, style = MonoSmall.copy(color = EngineDim))
                Box(Modifier.fillMaxWidth()) {
                    when (phase) {
                        ReadPhase.Composition -> CompositionReadSquare(x, counter)
                        ReadPhase.Placement -> PlacementReadSquare(x, counter)
                        ReadPhase.Layer -> LayerReadSquare(x, counter)
                    }
                }
                if (readouts) PhaseReadout(counter)
            }
        }
    }
}

@Composable
private fun Square(counter: PhaseCounter, color: Color, modifier: Modifier) {
    Box(
        modifier
            .countLayout(counter)
            .size(SquareSize)
            .clip(RoundedCornerShape(12.dp))
            .countDraw(counter)
            .background(color),
    )
}

@Composable
private fun CompositionReadSquare(x: State<Float>, counter: PhaseCounter) {
    counter.compositions++
    // Read here, in the body: this scope recomposes on every animation frame.
    Square(counter, EngineBad, Modifier.offset(x = x.value.dp))
}

@Composable
private fun PlacementReadSquare(x: State<Float>, counter: PhaseCounter) {
    counter.compositions++
    Square(
        counter,
        EngineAccent,
        Modifier.offset {
            counter.placementReads++
            IntOffset(x.value.dp.roundToPx(), 0) // read during placement only
        },
    )
}

@Composable
private fun LayerReadSquare(x: State<Float>, counter: PhaseCounter) {
    counter.compositions++
    Square(
        counter,
        EngineGood,
        Modifier.graphicsLayer {
            counter.layerReads++
            translationX = x.value.dp.toPx() // a layer property: no new recording
        },
    )
}
