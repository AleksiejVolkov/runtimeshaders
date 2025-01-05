package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.composables.MemoryCard
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.MutableRuntimeShaderState
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.fireShader
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimerShaderScreen(paddingValues: PaddingValues) {
    var startValue by remember { mutableStateOf(20) }
    var percentage by remember { mutableFloatStateOf(0.0f) }
    var isRunning by remember { mutableStateOf(false) }

    val shader = remember {
        MutableRuntimeShaderState(Shader(circularTimerShader).getRuntimeShader())
    }

    val seconds = remember { mutableStateOf(startValue) }

    LaunchedEffect(isRunning) {
        val startTime = System.currentTimeMillis()
        while (isRunning) {
            val elapsedTime = System.currentTimeMillis() - startTime // Elapsed time in milliseconds
            percentage = (elapsedTime / 1000f) / startValue // Normalize elapsed time to [0, 1]

            // Clamp percentage to [0, 1] to avoid overshooting
            percentage = percentage.coerceIn(0f, 1f)

            // Calculate remaining seconds
            seconds.value = ((startValue - (elapsedTime / 1000f))).toInt()

            delay(10) // Delay to control update frequency
            if (seconds.value < 0) {
                isRunning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = if (startValue != 0) startValue.toString() else "",
            onValueChange = {
                startValue = it.toIntOrNull() ?: 0
            },
            label = { Text("Start value") },
            modifier = Modifier.width(200.dp)
        )
        Box(
            modifier = Modifier.size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            ShadedBox(
                modifier = Modifier.size(250.dp),
                shader = shader.value,
                shaderUniforms = mapOf(
                    "percentage" to ShaderTypedValue.FloatType(percentage)
                ),
                includeTime = true
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Black.copy(alpha = 0.1f)),
                )
            }
            var previousSeconds by remember { mutableStateOf(seconds.value.coerceAtLeast(0)) }

            AnimatedContent(
                targetState = seconds.value.coerceAtLeast(0),
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(500)) { height -> -height } + fadeIn(
                        animationSpec = tween(
                            500
                        )
                    ))
                        .togetherWith(slideOutVertically(animationSpec = tween(500)) { height -> height } + fadeOut(
                            tween(500)
                        ))
                },
                label = "CountdownAnimation"
            ) { targetSeconds ->
                // Detect if transitioning
                val isTransitioning = targetSeconds != previousSeconds

                // Blur animation during transition
                val blurRadius by animateFloatAsState(
                    targetValue = if (isTransitioning) 30f else 0f, // Blur during transitions
                    animationSpec = tween(durationMillis = 500) // Match transition duration
                )

                // Update previousSeconds after transition completes
                LaunchedEffect(targetSeconds) {
                    previousSeconds = targetSeconds
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Apply animated blur effect
                            renderEffect = RenderEffect.createBlurEffect(
                                blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    text = targetSeconds.toString(),
                    fontSize = 50.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Thin,
                    color = Color.White
                )
            }
        }

        OutlinedButton(
            onClick = { isRunning = !isRunning },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (isRunning) "Stop" else "Start")
        }
    }
}

private val circularTimerShader = """
    
    vec2 cartesianToPolar(vec2 uv) {
        float r = length(uv);
        float theta = atan(uv.y, uv.x);
        return vec2(r, theta);
    }
    
    vec3 gradient(float t) {
        vec3 color1 = vec3(0.1, 0.2, 0.8); // Deep blue
        vec3 color2 = vec3(0.8, 0.5, 0.2); // Orange
        vec3 color3 = vec3(1.0, 1.0, 0.8); // Soft yellow

        if (t < 0.5) {
            float factor = smoothstep(0.0, 0.5, t);
            return mix(color1, color2, factor);
        } else {
            float factor = smoothstep(0.5, 1.0, t);
            return mix(color2, color3, factor);
        }
    }
    
    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
        
        vec3 grad = 0.5 + 0.5 * cos(time + uv.xyx + vec3(0, 2, 4));
        vec4 color = vec4(grad, 1.0); // Base color
        float width = 0.02;          // Bar width
        
        // Convert to polar coordinates
        vec2 polar = cartesianToPolar(uv);
        float r = polar.x;           // Radius
        float theta = polar.y;       // Angle
        
        float innerMask = step(0.3, r);
        
        // Normalize angle with zero at the top
        float normalizedAngle = mod((theta + 1.57079632679) / 6.28318530718, 1.0);
        
        // Fix percentage compensation
        float fixPercentage = percentage + percentage * 0.1;
        
        // Sample the wave mask at the center of each bar
        float barIndex = floor(theta * 10.0); // Determine bar index
        float centerTheta = (barIndex + 0.5) / 10.0; // Center angle of the bar
        float centerNormalizedAngle = mod((centerTheta + 1.57079632679) / 6.28318530718, 1.0);
        
        // Calculate wave mask at the center of the bar
        float val = smoothstep(fixPercentage, fixPercentage - 0.1, centerNormalizedAngle);
        float waveMask = smoothstep(0.32 + 0.1 * val, 0.31 + 0.1 * val, r);
        
        // Bar pattern with adjusted width
        float adjustedWidth = width / max(r, 0.001); // Compensate for radial scaling
        float linePattern = step( 0.5 - adjustedWidth, fract(theta * 10.0));
        
        // Apply the wave mask uniformly to the bar
        float barRadius = 0.3 + 0.1 * val; // Use wave mask to set bar radius
        float barMask = smoothstep(barRadius+0.01, barRadius, r);
        
        // Combine bar shape and inner mask
        float combinedMask = barMask * linePattern * innerMask;
        
        // Final color
        vec4 col = color * combinedMask * waveMask;
        
        return vec4(col.rgb + grad * 0.1, col.a + length(uv));
       }
""".trimIndent()