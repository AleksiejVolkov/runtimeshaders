package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.mutableRuntimeShaderStateOf
import org.intellij.lang.annotations.Language

@Composable
fun CircleBatteryIndicatorScreen(paddingValues: PaddingValues) {

    val shader = remember {
        mutableRuntimeShaderStateOf(
            Shader(circleBatteryIndicatorShader).getRuntimeShader(
                customFunctions = setOf(
                    ShaderFunction.NORMALIZECOORDINATES,
                    ShaderFunction.GETIMAGETEXTURE,
                    ShaderFunction.RANDOMSINEWAVE,
                )
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surface),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {

        ShadedBox(
            modifier = Modifier.size(200.dp),
            shader = shader.value,
            includeTime = true
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Red))
        }
    }
}

@Language("agsl")
private val circleBatteryIndicatorShader = """
    
        
    float GetDist(vec3 p) {
        vec4 sphere = vec4(0.,0.5,1.2,0.5);
        float dSphere = length(p-sphere.xyz)-sphere.w;
        float dPlane = p.y;
     //   return dSphere;
        float d = min(dSphere, dPlane);
        return d;
    }
    
     
    float RayMarch(vec3 rayOrigin, vec3 rayDirection) {
        float distanceOrigin = 0.;
        for (int i=0; i<100; i++) {
            vec3 p = rayOrigin + distanceOrigin*rayDirection;
            float dS = GetDist(p);
            distanceOrigin += dS;
            if(dS<0.01 || distanceOrigin > 100.) break;
        }
        
        return distanceOrigin;
    }

    vec3 GetNormal(vec3 p) {
        float d = GetDist(p);
        vec2 e = vec2(.01,0);
        
        vec3 n = d - vec3(
        GetDist(p-e.xyy),
        GetDist(p-e.yxy),
        GetDist(p-e.yyx));
        
        return normalize(n);
    }
    
    float GetLight(vec3 p) {
        vec3 lamp = vec3(0,5,6);
        lamp.xz = vec2(sin(time)*2.,cos(time)*2.);
        vec3 l = normalize(lamp - p);
        vec3 n = GetNormal(p);
        
        float dif = clamp(dot(n,l), 0.,1.);
        return dif;
        
        float d = RayMarch(p+n*0.02,l);
        if(d < length(lamp-p)) dif*=.1;
        
        return dif;
    }

     vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
        
        vec3 rO = vec3(0.0, .5, 0.2);
        vec3 rD = normalize(vec3(uv.x, -uv.y, 1.0));
        
        float d = RayMarch(rO, rD);
        vec3 p = rO + rD * d;
        vec3 n = GetNormal(p);
        
        float light = GetLight(p);
        
        vec3 color = vec3(.534+0.2*sin(time),.859+0.1*cos(time),.923)*light;
        float a=1.0-clamp(d,0.,1.);
        return vec4(color*a, a);
     }
""".trimIndent()