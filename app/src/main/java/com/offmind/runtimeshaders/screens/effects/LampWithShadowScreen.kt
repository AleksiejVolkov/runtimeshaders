package com.offmind.runtimeshaders.screens.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.lampWithShadowEffect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LampWithShadowScreen() {
    var percentage by remember { mutableFloatStateOf(0.15f) }
    var lampPair by remember { mutableStateOf(Offset.Zero) }
    var sliderPosition by remember { mutableStateOf(Offset.Zero) }
    var sliderWidth: Float by remember { mutableStateOf(0f) }
    var lampAngle: Float by remember { mutableStateOf(0f) }
    var lampWidth: Float by remember { mutableStateOf(0f) }

    val seekBarPx = with(LocalDensity.current) { 30.dp.roundToPx() }
    var checked by remember { mutableStateOf(true) }
    var rootSize by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootSize = Offset(it.size.width.toFloat(), it.size.height.toFloat())
            },
        contentAlignment = Alignment.TopEnd
    ) {
        val shader = remember {
            Shader(lampWithShadowEffect).getRuntimeShader(
                uniforms = basicUniformList
                    .addUniform(Uniform.Type.VEC2 to "lampPosition")
                    .addUniform(Uniform.Type.VEC2 to "seekBarPosition")
                    .addUniform(Uniform.Type.FLOAT to "lampAngle")
                    .addUniform(Uniform.Type.FLOAT to "lampWidth")
                    .addUniform(Uniform.Type.FLOAT to "sliderWidth")
            )
        }
        ShadedBox(
            shader = shader,
            shaderUniforms = mapOf(
                "percentage" to ShaderTypedValue.FloatType(if (checked) percentage else 0f),
                "lampPosition" to ShaderTypedValue.Vec2Type(lampPair.x, lampPair.y),
                "seekBarPosition" to ShaderTypedValue.Vec2Type(
                    sliderPosition.x,
                    sliderPosition.y
                ),
                "lampAngle" to ShaderTypedValue.FloatType(lampAngle),
                "lampWidth" to ShaderTypedValue.FloatType(lampWidth),
                "sliderWidth" to ShaderTypedValue.FloatType(sliderWidth)
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.generic_nature_1),
                contentDescription = "Sample Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Lamp(checked, rootSize) { position, angle, width ->
                lampPair = position
                lampAngle = angle
                lampWidth = width
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = percentage,
                onValueChange = { percentage = it },
                valueRange = 0.15f..1f,
                steps = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .onGloballyPositioned { coordinates ->
                                val ratio = rootSize.y / rootSize.x
                                val globalPosition = coordinates.positionInRoot()
                                val normalizedX =
                                    (globalPosition.x + seekBarPx / 2) / rootSize.x - 0.5f
                                val normalizedY =
                                    ((globalPosition.y + seekBarPx / 2) / rootSize.y - 0.5f) * ratio

                                sliderPosition = Offset(normalizedX, normalizedY)
                                sliderWidth = seekBarPx.toFloat() / rootSize.x
                            }
                    )
                }
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.8f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                )
            )
        }

    }
}

@Composable
private fun Lamp(
    isOn: Boolean,
    rootSize: Offset,
    onLampPositioned: (Offset, Float, Float) -> Unit = { _, _, _ -> }
) {
    val angleOffset = 5f
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val angle by infiniteTransition.animateFloat(
        initialValue = -angleOffset,
        targetValue = angleOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4000,
                easing = PendulumEasing
            ),
            repeatMode = RepeatMode.Reverse,
        ), label = ""
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(
                pivotFractionX = 0.5f,
                pivotFractionY = 0.0f,
            )
            rotationZ = angle
        }) {
        Box(
            modifier = Modifier
                .height(150.dp)
                .width(1.dp)
                .background(color = Color(0xFF2D2D2D))
        )
        LampBody(isOn, angle.toDouble(), rootSize) { pos, width ->
            onLampPositioned(pos, angle, width)
        }
    }
}

@Composable
private fun LampBody(
    isOn: Boolean,
    angle: Double,
    rootSize: Offset,
    onLampPositioned: (Offset, Float) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current

    val lampSizePx = with(density) { 32.dp.roundToPx() }
    //val ratio = screenHeightPx.toFloat() / screenWidthPx.toFloat()
    val ratio = rootSize.y / rootSize.x
    var lampWidth by remember { mutableStateOf(0f) }

    Box(contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.onSizeChanged {
            lampWidth = it.width.toFloat()
        }) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.lamp_inside_2),
                contentDescription = "Sample Image",
                contentScale = ContentScale.Crop,
                colorFilter = if (isOn) ColorFilter.tint(Color(0xFFFFFFFF)) else ColorFilter.tint(
                    Color(0xFF131313)
                )
            )
            Spacer(modifier = Modifier.size(10.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color = if (isOn) Color.White else Color(0x99141415))
                .onGloballyPositioned { coordinates ->
                    val globalPosition = coordinates.positionInRoot()
                    val angleInRadians = Math
                        .toRadians(-angle)
                        .toFloat()
                    val cosTheta = cos(Math.PI / 4.0 - angleInRadians).toFloat()
                    val sinTheta = sin(Math.PI / 4.0 - angleInRadians).toFloat()
                    val r = ((lampSizePx * sqrt(2.0)) / 2).toFloat()
                    val normalizedX = (globalPosition.x + r * cosTheta) / rootSize.x - 0.5f
                    val normalizedY =
                        ((globalPosition.y + r * sinTheta) / rootSize.y - 0.5f) * ratio

                    onLampPositioned(Offset(normalizedX, normalizedY), (lampWidth/rootSize.x))
                },
        )
        Column {
            Image(
                painter = painterResource(id = R.drawable.lamb_body_2),
                contentDescription = "Sample Image",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color(0xFF131313))
            )
            Spacer(modifier = Modifier.size(14.dp))
        }

    }
}

val PendulumEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.7f, 1.0f)
