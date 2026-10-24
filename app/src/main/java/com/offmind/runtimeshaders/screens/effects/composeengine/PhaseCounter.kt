package com.offmind.runtimeshaders.screens.effects.composeengine

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.delay

internal const val ENGINE_LOG_TAG = "ComposeEngine"

/**
 * How often each phase touched one element. The raw counters are plain fields, not snapshot state:
 * incrementing or reading them never invalidates anything, so counting does not change what is
 * counted. Only [perSecond], written once a second by [RecordPhases], is observable.
 */
internal class PhaseCounter(val name: String) {
    var compositions = 0
    var measures = 0
    var placements = 0
    var placementReads = 0
    var layerReads = 0
    var draws = 0
    var shaders = 0

    var perSecond by mutableStateOf(PhaseCounts())
        internal set

    fun snapshot() = PhaseCounts(
        compositions, measures, placements, placementReads, layerReads, draws, shaders,
    )
}

internal data class PhaseCounts(
    val compositions: Int = 0,
    val measures: Int = 0,
    val placements: Int = 0,
    val placementReads: Int = 0,
    val layerReads: Int = 0,
    val draws: Int = 0,
    val shaders: Int = 0,
) {
    operator fun minus(other: PhaseCounts) = PhaseCounts(
        compositions - other.compositions,
        measures - other.measures,
        placements - other.placements,
        placementReads - other.placementReads,
        layerReads - other.layerReads,
        draws - other.draws,
        shaders - other.shaders,
    )

    fun toLog() = "compose=$compositions measure=$measures place=$placements " +
        "placeLambda=$placementReads layerBlock=$layerReads draw=$draws newShader=$shaders"
}

/** Once a second: publishes each counter's rate and logs it, so a phone run can be read from logcat. */
@Composable
internal fun RecordPhases(screen: String, variant: String, counters: List<PhaseCounter>) {
    LaunchedEffect(screen, variant, counters) {
        var last = counters.map { it.snapshot() }
        while (true) {
            delay(1_000)
            val now = counters.map { it.snapshot() }
            counters.forEachIndexed { i, counter ->
                val rate = now[i] - last[i]
                counter.perSecond = rate
                Log.i(ENGINE_LOG_TAG, "phases screen=$screen variant=$variant element=${counter.name} ${rate.toLog()}")
            }
            last = now
        }
    }
}

/** Counts measure and placement passes of the node it is attached to. */
internal fun Modifier.countLayout(counter: PhaseCounter): Modifier = this then CountLayoutElement(counter)

/** Counts how often the content after it in the chain is recorded (drawn). */
internal fun Modifier.countDraw(counter: PhaseCounter): Modifier = this then CountDrawElement(counter)

// Elements compare by counter identity, so a recomposition never updates (and invalidates) the probe.
private data class CountLayoutElement(val counter: PhaseCounter) : ModifierNodeElement<CountLayoutNode>() {
    override fun create() = CountLayoutNode(counter)
    override fun update(node: CountLayoutNode) {
        node.counter = counter
    }
}

private class CountLayoutNode(var counter: PhaseCounter) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        counter.measures++
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            counter.placements++
            placeable.place(0, 0)
        }
    }
}

private data class CountDrawElement(val counter: PhaseCounter) : ModifierNodeElement<CountDrawNode>() {
    override fun create() = CountDrawNode(counter)
    override fun update(node: CountDrawNode) {
        node.counter = counter
    }
}

private class CountDrawNode(var counter: PhaseCounter) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        counter.draws++
        drawContent()
    }
}
