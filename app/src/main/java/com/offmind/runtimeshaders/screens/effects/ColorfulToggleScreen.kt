package com.offmind.runtimeshaders.screens.effects

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.graphics.RenderEffect
import androidx.compose.material3.Text
import androidx.compose.ui.draw.blur
import android.graphics.Shader as AndroidShader
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.offmind.runtimeshaders.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.sin

private val HandleSize = 50.dp
private val TrackWidth = 150.dp
private val TrackHeight = 70.dp
private val TrackPadding = 10.dp

@Composable
fun ColorfulToggleScreen(paddingValues: PaddingValues) {
    val shader = remember {
        Shader(colorfulToggleShader).getRuntimeShader().also {
            it.setFloatUniform("isBackground", 0f)
        }
    }
    val bgShader = remember {
        Shader(colorfulToggleShader).getRuntimeShader().also {
            it.setFloatUniform("isBackground", 1f)
        }
    }
    val handleShader = remember {
        Shader(handleBevelShader).getRuntimeShader()
    }

    var isToggleOn by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (isToggleOn) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 150f
        ),
        label = "toggleProgress"
    )

    val shaderPercent = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
    val handleColor = lerp(Color.White, Color.Black, progress)
    shader.setFloatUniform("percent", shaderPercent)
    bgShader.setFloatUniform("percent", shaderPercent)
    handleShader.setFloatUniform("percent", shaderPercent)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.1f
                    renderEffect = RenderEffect
                        .createBlurEffect(60f, 60f, AndroidShader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
        ) {
            ShadedBox(
                modifier = Modifier.fillMaxSize(),
                shader = bgShader,
                includeTime = true
            )
        }

        Box(
            modifier = Modifier
                .width(TrackWidth)
                .height(TrackHeight)
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(50.dp))
                .clip(RoundedCornerShape(50.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isToggleOn = !isToggleOn }
        ) {
            ShadedBox(
                modifier = Modifier.fillMaxSize(),
                shader = shader,
                includeTime = true
            )

            BoxWithConstraints(modifier = Modifier.padding(TrackPadding)) {
                val travelDistance = with(LocalDensity.current) {
                    (maxWidth - HandleSize).toPx()
                }
                ToggleHandle(progress = progress, travelDistance = travelDistance, color = handleColor, shader = handleShader)
            }
        }
    }
}

@Composable
fun ToggleHandle(
    progress: Float,
    travelDistance: Float,
    shader: android.graphics.RuntimeShader,
    color: Color = Color.White,
) {
    val squishY = 1f - sin(progress * PI.toFloat()) * 0.3f
    val squishX = 1f + sin(progress * PI.toFloat()) * 0.3f

    Box(
        modifier = Modifier
            .size(HandleSize)
            .graphicsLayer {
                translationX = progress * travelDistance
                scaleY = squishY
                scaleX = squishX
            }
            .background(color, shape = CircleShape)
            .clip(CircleShape)
    ) {
        ShadedBox(
            modifier = Modifier.fillMaxSize(),
            shader = shader,
            includeTime = true
        )
    }
}

@Language("AGSL")
private val colorfulToggleShader = """

    uniform float percent;
    uniform float isBackground;

    float arcGlow(vec2 uv, float r, float angOff, float halfSpan, float sharp) {
        float d = abs(length(uv) - r);
        float glow = exp(-d * d * sharp);
        float theta = mod(atan(uv.y, uv.x) - angOff + 9.4248, 6.2832) - 3.1416;
        float mask = smoothstep(halfSpan, halfSpan * 0.5, abs(theta));
        return glow * mask;
    }
    
    float radialGlow(vec2 uv, float r, float sharp) {
        float d = abs(length(uv) - r);
        return exp(-d * d * sharp);
    }

    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);

        // ring center under the handle in "on" position
        vec2 suv = uv - vec2(0.55, 0.0);

        vec3 col = vec3(0.0);

        // strand 1: blue, CCW
        col += arcGlow(suv, 0.52,  time * 0.40,         1.30, 230.) * HSVtoRGB(vec3(0.62, 1.0, 1.8));
        // strand 2: purple, CW
        col += arcGlow(suv, 0.49, -time * 0.50 + 1.57,  1.10, 270.) * HSVtoRGB(vec3(0.74, 0.9, 1.5));
        // strand 3: cyan, CCW faster
        col += arcGlow(suv, 0.505, time * 0.70 + 3.14,  1.50, 200.) * HSVtoRGB(vec3(0.55, 0.8, 1.3));
        // strand 4: pink, CW slow
        col += arcGlow(suv, 0.515,-time * 0.35 + 4.71,  1.00, 300.) * HSVtoRGB(vec3(0.87, 1.0, 1.2));

        // base halo
        col += radialGlow(suv, 0.508, 50.) * 0.3 * vec3(0.3, 0.4, 1.0);

        // full-background tint: radial glow only, no arc masking
        vec3 bg = radialGlow(suv, 0.505, 2.) * HSVtoRGB(vec3(0.55, 0.8, 1.3));
        bg    += radialGlow(suv, 0.49,  2.) * HSVtoRGB(vec3(0.74, 0.9, 1.5));
        col += bg * 0.2;

        // sparkles orbiting the ring
        for (int i = 0; i < 5; i++) {
            float seed = float(i) * 1.618;
            float a = time * (0.8 + seed * 0.4) + seed * 6.28;
            float rr = 0.508 + (Hash21(vec2(seed, 0.1)) - 0.5) * 0.04;
            vec2 pos = rr * vec2(cos(a), sin(a));
            float dd = length(suv - pos);
            col += exp(-dd * dd * 500.) * vec3(1.0) * 0.7;
        }

        float vignette = mix(1.0, 1.0 - CubicOut(clamp(length(uv) / 0.6, 0.0, 1.0)), isBackground);
        col = clamp(col, 0.0, 3.0) * percent * vignette;
        return vec4(col, 1.0);
    }
""".trimIndent()

@Language("AGSL")
private val handleBevelShader = """

    uniform float percent;
    
     float arcGlow(vec2 uv, float r, float angOff, float halfSpan, float sharp) {
        float d = abs(length(uv) - r);
        float glow = exp(-d * d * sharp);
        float theta = mod(atan(uv.y, uv.x) - angOff + 9.4248, 6.2832) - 3.1416;
        float mask = smoothstep(halfSpan, halfSpan * 0.5, abs(theta));
        return glow * mask;
    }

    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        
        // ring center under the handle in "on" position
        vec2 suv = uv * vec2(1.2);

        vec3 col = vec3(0.0);

        // strand 1: blue, CCW
        col += arcGlow(suv, 0.52,  time * 0.40,         1.30, 230.) * HSVtoRGB(vec3(0.62, 1.0, 1.8));
        // strand 2: purple, CW
        col += arcGlow(suv, 0.49, -time * 0.50 + 1.57,  1.10, 270.) * HSVtoRGB(vec3(0.74, 0.9, 1.5));
        // strand 3: cyan, CCW faster
        col += arcGlow(suv, 0.505, time * 0.70 + 3.14,  1.50, 200.) * HSVtoRGB(vec3(0.55, 0.8, 1.3));
        // strand 4: pink, CW slow
        col += arcGlow(suv, 0.515,-time * 0.35 + 4.71,  1.00, 300.) * HSVtoRGB(vec3(0.87, 1.0, 1.2));

        // thin ring at the handle edge
        float d = abs(length(uv) - 0.45);
        float ring = exp(-d * d * 9000.0);

        // top-left arc: screen-space top = negative y, left = negative x
        // center angle ≈ -2.0 rad (between 9 o'clock and 12 o'clock)
        float theta = mod(atan(uv.y, uv.x) + 2.0 + 9.4248, 6.2832) - 3.1416;
        float arcMask = 1.0 - smoothstep(0.4, 0.9, abs(theta));

        float intensity = ring * arcMask * percent;
        return vec4(col * intensity, intensity);
    }
""".trimIndent()

