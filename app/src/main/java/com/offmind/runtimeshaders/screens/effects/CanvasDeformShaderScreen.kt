package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

val ElasticOutEasing = Easing { t ->
    val p = 0.3f
    if (t == 0f || t == 1f) t
    else {
        val s = p / 4
        2f.pow(-10f * t) * sin((t - s) * (2f * PI.toFloat()) / p) + 1f
    }
}

@Composable
fun CanvasDeformScreen(paddingValues: PaddingValues) {
    val shader = remember { RuntimeShader(runtimeShader) }
    var targetPercentage by remember { mutableFloatStateOf(0f) }
    val percentage = animateFloatAsState(targetValue = targetPercentage, animationSpec = tween(durationMillis = 1000, easing = ElasticOutEasing))
    var fingerPosition by remember { mutableStateOf(Offset.Zero) }
    var fingerStartPosition by remember { mutableStateOf(Offset.Zero) }

    val actions = listOf("Cut", "Copy", "Paste", "Edit")
    val pressed = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .onSizeChanged { size ->
                    shader.setFloatUniform(
                        "resolution", size.width.toFloat(), size.height.toFloat()
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            pressed.value = change.pressed

                            if (change.pressed) {
                                if (change.previousPressed.not()) {
                                    fingerStartPosition = change.position
                                }
                                targetPercentage = 1f
                                fingerPosition = change.position - fingerStartPosition
                            } else {
                                targetPercentage = 0f
                            }

                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .graphicsLayer {
                    if (pressed.value) {
                        shader.setFloatUniform("percentage", 1f)
                    } else {
                        shader.setFloatUniform("percentage", percentage.value)
                    }
                    shader.setFloatUniform("touch", fingerPosition.x, fingerPosition.y)

                    this.renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, "image")
                        .asComposeRenderEffect()
                }
                .clickable {
                    targetPercentage = if (targetPercentage == 0f) 1f else 0f
                }
        ) {
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .background(Color(0x8843484C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                actions.forEach { action ->
                    Text(
                        text = action,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                    if(action != "Edit") {
                        HorizontalDivider()
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private val runtimeShader = """
    uniform shader image;
    uniform float2 resolution;
    uniform float percentage;
    uniform float2 touch;
        
     vec4 GetImageTexture(vec2 p, vec2 pivot, vec2 r) {
        p.x /= r.x / r.y;
        p += pivot;
        p *= r;
        return image.eval(p);
    } 

    half4 main(float2 fragCoord) {
        float ratio = resolution.x / resolution.y;
        float2 uv = fragCoord / resolution - 0.5;
        uv.x *= ratio;
        vec2 nMouse = touch / resolution;
        nMouse.x *= ratio;
        
        vec2 scale = vec2(min(length(nMouse.x), 0.3), min(length(nMouse.y), 0.4));
        float influence = dot(normalize(nMouse), uv); 

        influence = smoothstep(0., 1.5, influence); 
        
        scale *= influence;
        scale *= percentage;
     
        uv *= vec2(1.0) - scale;

        vec4 img = GetImageTexture(uv, vec2(0.5), resolution);
       
        return half4(img.rgb, img.a);
        
    }
""".trimIndent()