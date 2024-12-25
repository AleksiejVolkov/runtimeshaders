@file:OptIn(ExperimentalMaterial3Api::class)

package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.shaders.Shader
import org.intellij.lang.annotations.Language

@Composable
fun SnowDialogScreen() {
    var showDialog by remember { mutableStateOf(false) }
    val snowFlakesShader = remember {
        Shader(snowShader).getRuntimeShader(
            customFunctions = setOf(
                ShaderFunction.NORMALIZECOORDINATES,
                ShaderFunction.GETIMAGETEXTURE,
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
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
            /*ShadedBox(
               modifier = Modifier
                   .fillMaxSize(),
                shader = snowFlakesShader,
                includeTime = true
            ) {
                Box(modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(alpha = 0.1f)))
            }*/
            SnowedDialog {
                showDialog = false
            }
        }
    }
}

@Composable
private fun SnowedDialog(
    onDismiss: () -> Unit
) {
    val dialogSnowShader = remember {
        Shader(dialogSnowBottom).getRuntimeShader(
            customFunctions = setOf(
                ShaderFunction.NORMALIZECOORDINATES,
                ShaderFunction.GETIMAGETEXTURE,
                ShaderFunction.RANDOMSINEWAVE,
            )
        )
    }

    val snowFlakesShader = remember {
        Shader(snowShader).getRuntimeShader(
            customFunctions = setOf(
                ShaderFunction.NORMALIZECOORDINATES,
                ShaderFunction.GETIMAGETEXTURE,
            )
        )
    }

    val timeState = provideTimeAsState()
    dialogSnowShader.setFloatUniform("time", timeState.value)
    LaunchedEffect(timeState) {
        dialogSnowShader.setFloatUniform("time", timeState.value)
        snowFlakesShader.setFloatUniform("time", timeState.value)
    }

    BasicAlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier
            .onSizeChanged { size ->
                dialogSnowShader.setFloatUniform(
                    "resolution",
                    size.width.toFloat(),
                    size.height.toFloat()
                )
            }
            .graphicsLayer {
                this.renderEffect = RenderEffect
                    .createRuntimeShaderEffect(dialogSnowShader, "image")
                    .asComposeRenderEffect()
            }
    ) {
        Column(
            Modifier
                .background(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface)
                .padding(16.dp)
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

@Language("agsl")
private val dialogSnowBottom = """
    
      vec4 imageWithAlpha(vec4 image, vec2 uv, vec2 resolution) {
           vec4 correcteImage = image;
           float ratio = resolution.x / resolution.y;
           if (abs(uv.x) > 0.5 * ratio || abs(uv.y) > 0.5) {
               correcteImage.a = 0.0;
           }
            return correcteImage;
       }
       
       vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
            vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
            image = imageWithAlpha(image, uv, resolution);
            
            float ratio = resolution.x / resolution.y;
            
            
            float amplitude = clamp(time*0.5, 0.0,1.);
           // amplitude = 0.0;
            float wave = RandomSinWave(uv.x, 10., 0.1, 0.6)*amplitude;
      
            float snowMask = uv.y-0.48 < wave*0.4*length(uv.x*uv.x) ? 1.0 : .0;
            
            float topBound = 0.5+(sin(10.*uv.x)*0.5+0.5)*0.01*(10.-10.*amplitude)-0.1*length(uv.x*uv.x);
            float leftBound = -.5*ratio+(sin(10.*uv.y)*0.5+0.5)*0.5*length(uv.y-topBound);
            float rightBound = .5*ratio-(sin(10.*uv.y)*0.5+0.5)*0.4*length(uv.y-topBound);
            
            vec4 snowArea = vec4(leftBound, rightBound, topBound, 1.0);
            
            vec4 snow = vec4(0.0);
            
            if(uv.x > snowArea.x && uv.x < snowArea.y && uv.y > snowArea.z && uv.y < snowArea.w) {
                snow = snowMask*vec4(vec3(1.0),1.0);
              // snow = vec4(vec3(.5),1.0);
            }
            
            //vec3 finalColor = mix(image.rgb, vec3(1.0), snowMask);
            vec4 finalColor = mix(image, snow, snow.a);
            
            return vec4(finalColor.rgb*finalColor.a, finalColor.a);
       }
""".trimIndent()

@Language("agsl")
val snowShader = """
   const int LAYERS = 15; // Reduced layers for better performance
   const float DEPTH = 0.15; // Increased depth per layer to compensate for fewer layers
   const float WIDTH = 0.4;
   const float SPEED = 1.0;

   vec4 main(float2 fragCoord) {
       float2 uv = NormalizeCoordinates(fragCoord, resolution);  
       const mat3 p = mat3(13.323122, 23.5112, 21.71123, 21.1212, 28.7312, 11.9312, 21.8112, 14.7212, 61.3934);

       vec3 acc = vec3(0.0);
       float alpha = 0.0; // Initialize alpha
       float dof = 5.0 * sin(time * 0.1);

       for (int i = 0; i < LAYERS; i++) {
           float fi = float(i);
           vec2 q = uv * (1.0 + fi * DEPTH);
           
           // Adjust flake position with modulation and time
           q -= vec2(q.y * (WIDTH * mod(fi * 7.238917, 1.0) - WIDTH * 0.5), SPEED * time / (1.0 + fi * DEPTH * 0.03));
           
           vec3 n = vec3(floor(q), 31.189 + fi);
           vec3 m = floor(n) * 0.00001 + fract(n);
           vec3 mp = (31415.9 + m) / fract(p * m);
           vec3 r = fract(mp);
           
           // Rounded snowflake shape using a circular mask
           float2 center = mod(q, 1.0) - 0.5 + 0.5 * r.xy;
           float distanceToCenter = length(center); // Circular distance
           float flakeRadius = 0.02 + 0.01 * r.z; // Vary radius slightly per flake

           // Smoother edges with extended smoothstep
           float intensity = smoothstep(flakeRadius + 0.01, flakeRadius, distanceToCenter) * smoothstep(flakeRadius, flakeRadius - 0.01, distanceToCenter);

           // Ensure flakes are white or transparent (prevent black color)
           vec3 flakeColor = vec3(1.0); // White color for flakes
           acc += flakeColor * intensity;

           // Accumulate alpha with smooth transition
           alpha += intensity;
       }

       // Normalize alpha to ensure it doesn’t exceed 1.0
       alpha = clamp(alpha, 0.0, 1.0);

       // Return the final color with proper transparency
       return vec4(acc, alpha);
   }
""".trimIndent()

