package com.offmind.runtimeshaders.screens.effects.metalknob

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.removeUniform
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

private val KnobRadius = 110.dp
private val Background = Color(0xFFEAEAED)
private const val Sweep = (1.5 * PI).toFloat()
private const val TickIntervals = 54

@Composable
fun MetalKnobScreen() {
    val haptics = LocalHapticFeedback.current
    val knobRadiusPx = with(LocalDensity.current) { KnobRadius.toPx() }
    var value by remember { mutableFloatStateOf(0.62f) }
    val shader = remember {
        Shader(metalKnobShader).getRuntimeShader(
            uniforms = basicUniformList
                .removeUniform("image")
                .addUniform(Uniform.Type.FLOAT to "knobRadius")
                .addUniform(Uniform.Type.FLOAT to "knobAngle")
                .addUniform(Uniform.Type.FLOAT to "knobValue")
                .addUniform(Uniform.Type.VEC3 to "lightDir"),
            customFunctions = emptySet(),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val previous = change.previousPosition - center
                    val current = change.position - center
                    var delta = atan2(current.y, current.x) - atan2(previous.y, previous.x)
                    if (delta > PI) delta -= (2 * PI).toFloat()
                    if (delta < -PI) delta += (2 * PI).toFloat()
                    val next = (value + delta / Sweep).coerceIn(0f, 1f)
                    if ((next * TickIntervals).roundToInt() != (value * TickIntervals).roundToInt()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    value = next
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        ShadedBox(
            modifier = Modifier.fillMaxSize(),
            shader = shader,
            shaderUniforms = mapOf(
                "knobRadius" to ShaderTypedValue.FloatType(knobRadiusPx),
                "knobAngle" to ShaderTypedValue.FloatType((value - 0.5f) * Sweep),
                "knobValue" to ShaderTypedValue.FloatType(value),
                // for now the lamp is fixed: up and to the left, a bit in front of the screen
                "lightDir" to ShaderTypedValue.Vec3Type(-0.45f, -0.6f, 0.66f),
            ),
        )

        Text(
            text = "Drag around the knob to turn it.",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp, start = 32.dp, end = 32.dp),
            color = Color(0xFF8A8A90),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier.offset(y = KnobRadius * 1.72f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "VOLUME",
                color = Color(0xFF4A4A50),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
            )
            Text(
                text = "${(value * 100).roundToInt()}",
                color = Color(0xFF9A9AA0),
                fontSize = 13.sp,
            )
        }
    }
}
