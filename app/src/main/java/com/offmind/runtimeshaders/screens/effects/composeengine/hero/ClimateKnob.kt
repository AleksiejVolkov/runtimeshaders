package com.offmind.runtimeshaders.screens.effects.composeengine.hero

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.screens.effects.composeengine.PhaseCounter
import com.offmind.runtimeshaders.screens.effects.composeengine.countDraw
import com.offmind.runtimeshaders.screens.effects.metalknob.metalKnobShader
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.removeUniform
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Where the knob's angle is read — the three variants the film compares. */
internal enum class KnobRead(val label: String) {
    Composition("Read in composition"),
    Placement("Read in placement"),
    Draw("Read in draw"),
}

internal const val KNOB_SWEEP = (1.5 * PI).toFloat()   // 270°

/** The cabin lamp: fixed above and to the left, so the highlight stays put while the knob turns. */
private val CabinLight = floatArrayOf(-0.42f, -0.58f, 0.70f)

// The Metal Knob shader with one more uniform: the notch can be left to a separate view.
private val climateKnobShader = metalKnobShader
    .replace("float notch = smoothstep", "float notch = notchVisible * smoothstep")

/**
 * The climate knob: the brushed-steel shader of the Metal Knob demo on a round pod. Dragging around
 * it turns [value] (0…1). [readIn] chooses where the angle is read: in composition (the body reads
 * it and hands it to the draw lambda), in placement (the notch is its own small view, rotated when
 * placed) or in draw (the draw lambda reads it). [counter] counts what each variant runs.
 */
@Composable
internal fun ClimateKnob(
    value: MutableFloatState,
    readIn: KnobRead,
    counter: PhaseCounter,
    modifier: Modifier = Modifier,
    radius: Dp = 64.dp,
) {
    val shader = remember {
        Shader(climateKnobShader).getRuntimeShader(
            uniforms = basicUniformList
                .removeUniform("image")
                .addUniform(Uniform.Type.FLOAT to "knobRadius")
                .addUniform(Uniform.Type.FLOAT to "knobAngle")
                .addUniform(Uniform.Type.FLOAT to "knobValue")
                .addUniform(Uniform.Type.VEC3 to "lightDir")
                .addUniform(Uniform.Type.FLOAT to "notchVisible"),
            customFunctions = emptySet(),
        )
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    counter.compositions++

    // Composition: the body reads the value, so every change recomposes this function.
    val composedValue = if (readIn == KnobRead.Composition) value.floatValue else 0f

    Box(
        modifier
            .size(radius * 3.1f)
            .clip(CircleShape)
            .pointerInput(value) {
                detectDragGestures { change, _ ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val prev = change.previousPosition - c
                    val cur = change.position - c
                    var delta = atan2(cur.y, cur.x) - atan2(prev.y, prev.x)
                    if (delta > PI) delta -= (2 * PI).toFloat()
                    if (delta < -PI) delta += (2 * PI).toFloat()
                    value.floatValue = (value.floatValue + delta / KNOB_SWEEP).coerceIn(0f, 1f)
                }
            }
            .countDraw(counter)
            .drawBehind {
                val v = when (readIn) {
                    KnobRead.Composition -> composedValue
                    KnobRead.Draw -> value.floatValue      // read while drawing
                    KnobRead.Placement -> 0.5f             // the knob body ignores it; the notch view turns
                }
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("knobRadius", radiusPx)
                shader.setFloatUniform("knobAngle", (v - 0.5f) * KNOB_SWEEP)
                shader.setFloatUniform("knobValue", v)
                shader.setFloatUniform("lightDir", CabinLight[0], CabinLight[1], CabinLight[2])
                shader.setFloatUniform("notchVisible", if (readIn == KnobRead.Placement) 0f else 1f)
                drawRect(brush)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (readIn == KnobRead.Placement) Notch(value, radiusPx, radius, counter)
    }
}

/** The notch as its own small view, placed and rotated in the placement phase. */
@Composable
private fun Notch(value: MutableFloatState, knobRadiusPx: Float, radius: Dp, counter: PhaseCounter) {
    Box(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    counter.placementReads++
                    val angle = (value.floatValue - 0.5f) * KNOB_SWEEP     // read while placing
                    val r = knobRadiusPx * 0.665f
                    placeable.placeWithLayer(
                        (sin(angle) * r).roundToInt(),
                        (-cos(angle) * r).roundToInt(),
                    ) { rotationZ = angle * 180f / PI.toFloat() }
                }
            }
            .size(radius * 0.035f, radius * 0.15f)    // the shader's notch: 0.59…0.74 R
            .clip(RoundedCornerShape(radius * 0.0175f))
            .background(Color(0xFF2A2B30)),
    )
}
