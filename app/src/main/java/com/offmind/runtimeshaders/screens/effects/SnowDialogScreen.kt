@file:OptIn(ExperimentalMaterial3Api::class)

package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.mutableRuntimeShaderStateOf
import org.intellij.lang.annotations.Language
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun SnowDialogScreen(paddingValues: PaddingValues) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.christmas_night),
            contentDescription = "Sample Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!showDialog) {
            Button(onClick = {
                showDialog = true
            }) {
                Text("Ho-ho-ho!")
            }
        } else {
            SnowedDialog(paddingValues = paddingValues) {
                showDialog = false
            }
        }
    }
}

@Composable
private fun SnowedDialog(
    paddingValues: PaddingValues,
    onDismiss: () -> Unit
) {
    val dialogSnowShader = remember {
        mutableRuntimeShaderStateOf(
            Shader(dialogSnowBottom).getRuntimeShader(
                customFunctions = setOf(
                    ShaderFunction.NORMALIZECOORDINATES,
                    ShaderFunction.GETIMAGETEXTURE,
                    ShaderFunction.RANDOMSINEWAVE,
                )
            )
        )
    }

    val flakesShader = remember {
        mutableRuntimeShaderStateOf(Shader(snowShader).getRuntimeShader(
            customFunctions = setOf(
                ShaderFunction.NORMALIZECOORDINATES,
                ShaderFunction.GETIMAGETEXTURE,
            )
        ))
    }

    val flakesShader2 = remember {
        mutableRuntimeShaderStateOf(Shader(snowShader).getRuntimeShader(
            customFunctions = setOf(
                ShaderFunction.NORMALIZECOORDINATES,
                ShaderFunction.GETIMAGETEXTURE,
            )
        ))
    }

    val timeState = provideTimeAsState()

    flakesShader.update {
        setIntUniform("uLayers", 5)
        setFloatUniform("uDepth", 0.15f)
        setFloatUniform("uSpeed", 1.0f)
    }

    flakesShader2.update {
        setIntUniform("uLayers", 15)
        setFloatUniform("uDepth", 0.2f)
        setFloatUniform("uSpeed", 0.8f)
    }

    LaunchedEffect(timeState.value) {
        dialogSnowShader.update {
            setFloatUniform("time", timeState.value)
        }
        flakesShader.update{
            setFloatUniform("time", timeState.value)
        }
        flakesShader2.update {
            setFloatUniform("time", timeState.value)
        }
    }

    BasicAlertDialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    flakesShader.value.setFloatUniform(
                        "resolution",
                        size.width.toFloat(),
                        size.height.toFloat()
                    )
                }
                .graphicsLayer {
                    this.renderEffect = RenderEffect
                        .createRuntimeShaderEffect(flakesShader.value, "image")
                        .asComposeRenderEffect()
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        flakesShader2.value.setFloatUniform(
                            "resolution",
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                    }
                    .graphicsLayer {
                        this.renderEffect = RenderEffect
                            .createRuntimeShaderEffect(flakesShader2.value, "image")
                            .asComposeRenderEffect()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Black.copy(0.1f))
                )
            }
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .background(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface
                    )
                    .onSizeChanged { size ->
                        dialogSnowShader.value.setFloatUniform(
                            "resolution",
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                    }
                    .graphicsLayer {
                        println("HUI gl called")
                        this.renderEffect = RenderEffect
                            .createRuntimeShaderEffect(dialogSnowShader.value, "image")
                            .asComposeRenderEffect()
                    }
                    .padding(16.dp),
            ) {
                Text("Merry Christmas!", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Happy New Year!")
                Spacer(modifier = Modifier.height(26.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(5.dp),
                        text = "Close",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Language("agsl")
private val dialogSnowBottom = """
    
     vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
            vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
            
            float ratio = resolution.x / resolution.y;
           
            float amplitude = clamp(time*0.5, 0.0,1.);
            float wave = RandomSinWave(uv.x, 10., 0.1, 0.6)*amplitude;
      
            float snowMask = uv.y-0.48 < wave*0.4*length(uv.x*uv.x) ? 1.0 : .0;
            
            float topBound = 0.5+(sin(10.*uv.x)*0.5+0.5)*0.01*(10.-10.*amplitude)-0.1*length(uv.x*uv.x);
            float leftBound = -.5*ratio+(sin(10.*uv.y)*0.5+0.5)*0.5*length(uv.y-topBound);
            float rightBound = .5*ratio-(sin(10.*uv.y)*0.5+0.5)*0.4*length(uv.y-topBound);
            
            vec4 snowArea = vec4(leftBound, rightBound, topBound, 1.0);
            
            vec4 snow = vec4(0.0);
            
            if(uv.x > snowArea.x && uv.x < snowArea.y && uv.y > snowArea.z && uv.y < snowArea.w) {
                snow = snowMask*vec4(vec3(1.0),1.0);
            }
            
            vec4 finalColor = mix(image, snow, snow.a);
            
            return vec4(finalColor.rgb*finalColor.a, finalColor.a);
       }
""".trimIndent()

@Language("agsl")
val snowShader = """
   uniform int uLayers;
   uniform float uDepth;
   uniform float uSpeed;
   
   const int MAX_LAYERS = 50;
   const float WIDTH = 0.4;
   

   vec4 main(float2 fragCoord) {
       float2 uv = NormalizeCoordinates(fragCoord, resolution);  
       vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
       const mat3 p = mat3(13.323122, 23.5112, 21.71123, 21.1212, 28.7312, 11.9312, 21.8112, 14.7212, 61.3934);
       
       float ratio = resolution.y / resolution.x;

       vec3 acc = vec3(0.0);
       float alpha = 0.0; // Initialize alpha
       float dof = 5.0 * sin(time * 0.1);

       for (int i = 0; i < MAX_LAYERS; i++) {
           if (i >= uLayers) break; // Break out of the loop if i exceeds uLayers
         
           float fi = float(i);
           vec2 q = uv * (1.0 + fi * uDepth);
           
           // Adjust flake position with modulation and time
           q -= vec2(q.y * (WIDTH * mod(fi * 7.238917, 1.0) - WIDTH * 0.5), uSpeed * time / (1.0 + fi * uDepth * 0.03));
           
           vec3 n = vec3(floor(q), 31.189 + fi);
           vec3 m = floor(n) * 0.00001 + fract(n);
           vec3 mp = (31415.9 + m) / fract(p * m);
           vec3 r = fract(mp);
           
           // Rounded snowflake shape using a circular mask
           float2 center = mod(q, 1.0) - 0.5 + 0.5 * r.xy;
           float distanceToCenter = length(center); // Circular distance
           float flakeRadius = 0.015 + 0.01 * r.z; // Vary radius slightly per flake

           // Smoother edges with extended smoothstep
           float intensity = smoothstep(flakeRadius + 0.015, flakeRadius, distanceToCenter) * 
                               smoothstep(flakeRadius, flakeRadius - 0.015, distanceToCenter);

           // Ensure flakes are white or transparent (prevent black color)
           vec3 flakeColor = vec3(1.0); // White color for flakes
           acc += flakeColor * intensity;

           // Accumulate alpha with smooth transition
           alpha += intensity;
       }

       // Normalize alpha to ensure it doesn’t exceed 1.0
       alpha = clamp(alpha, 0.0, 1.0);

       vec3 finalColor = mix(image.rgb, acc, alpha);
       
       if(uv.y < -0.5*ratio || uv.y > .5*ratio) {
           finalColor = vec3(0.0);
           alpha = 0.0;
       }
       return vec4(finalColor, alpha+image.a);
   }
""".trimIndent()

