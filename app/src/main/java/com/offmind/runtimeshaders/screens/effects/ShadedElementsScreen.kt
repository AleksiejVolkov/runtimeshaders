package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.shaders.Shader
import org.intellij.lang.annotations.Language

@Composable
fun ShadedElementsScreen(paddingValues: PaddingValues) {
    val shader = remember {
        Shader(globalLightShadowShader).getRuntimeShader(
            customFunctions = setOf(ShaderFunction.CUBICOUT)
        )
    }

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFBDBDBD), Color(0xFFE5E5E5)))),
        contentAlignment = Alignment.Center
    ) {
        ShadedBox(
            modifier = Modifier.size(width = 340.dp, height = 180.dp),
            shader = shader
        ) {
            Box(
                modifier = Modifier.background(color = Color.White, shape = RoundedCornerShape(24.dp))
            ) {
                Text(
                    text = "Shaded Elements Screen",
                    color = Color.White
                )
            }
        }
    }
}

@Language("AGSL")
private val globalLightShadowShader = """
    vec4 sampleImage(vec2 coord) {
        if (coord.x < 0.0 || coord.y < 0.0 || coord.x > resolution.x || coord.y > resolution.y) {
            return vec4(0.0);
        }
        return image.eval(coord);
    }

    vec4 weightedShadowSample(vec2 coord) {
        vec2 shadowOffset = vec2(20.0, 24.0);
        vec2 origin = coord - shadowOffset;
        float blurRadius = 28.0;

        vec4 shadow = vec4(0.0);
        vec4 sample = vec4(0.0);

        sample = sampleImage(origin + vec2(-1.00, -1.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.045;
        sample = sampleImage(origin + vec2( 0.00, -1.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.060;
        sample = sampleImage(origin + vec2( 1.00, -1.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.045;
        sample = sampleImage(origin + vec2(-1.00,  0.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.060;
        sample = sampleImage(origin);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.220;
        sample = sampleImage(origin + vec2( 1.00,  0.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.060;
        sample = sampleImage(origin + vec2(-1.00,  1.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.045;
        sample = sampleImage(origin + vec2( 0.00,  1.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.060;
        sample = sampleImage(origin + vec2( 1.00,  1.00) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.045;

        sample = sampleImage(origin + vec2(-0.50, -0.50) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.065;
        sample = sampleImage(origin + vec2( 0.50, -0.50) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.065;
        sample = sampleImage(origin + vec2(-0.50,  0.50) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.065;
        sample = sampleImage(origin + vec2( 0.50,  0.50) * blurRadius);
        shadow += vec4(sample.rgb * sample.a, sample.a) * 0.065;

        vec3 shadowColor = shadow.rgb / max(shadow.a, 0.001);
        float falloff = 1.0 - CubicOut(clamp(1.0 - shadow.a, 0.0, 1.0));
        return vec4(shadowColor, falloff * 0.42);
    }

    vec4 main(float2 fragCoord) {
        vec4 source = sampleImage(fragCoord);
        vec4 shadow = weightedShadowSample(fragCoord);
        shadow.a *= 1.0 - source.a;

        vec3 color = mix(shadow.rgb, source.rgb, source.a);
        float alpha = source.a + shadow.a;

        return vec4(color, alpha);
    }
""".trimIndent()
