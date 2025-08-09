package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language

@Composable
fun TestShaderScreen(paddingValues: PaddingValues) {
    val shader = remember { RuntimeShader(metaballShader) }
    val parentBoxSize = remember { mutableStateOf(IntSize.Zero) }
    val firstButtonPosition = remember { mutableStateOf(Offset.Zero) }
    val secondButtonPosition = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(Color(0xFF171717))
            .onSizeChanged {
                parentBoxSize.value = it
            },
        contentAlignment = Alignment.Center
    ) {
        ShadedButton(
            modifier = Modifier
                .padding(start = 90.dp)
                .size(50.dp)
                .onGloballyPositioned {
                    firstButtonPosition.value = it.positionInParent() +
                            Offset(
                                x = it.size.width * 0.5f,
                                y = it.size.height * 0.5f
                            )
                },
            icon = Icons.Filled.Favorite,
            shader = shader,
            parentBoxSize = parentBoxSize.value,
            otherViewPosition = secondButtonPosition.value,
            myPosition = firstButtonPosition.value,
        )
        ShadedButton(
            modifier = Modifier
                .padding(end = 90.dp)
                .size(50.dp)
                .onGloballyPositioned {
                    secondButtonPosition.value = it.positionInParent() +
                            Offset(
                                x = it.size.width * 0.5f,
                                y = it.size.height * 0.5f
                            )
                },
            icon = Icons.Filled.Star,
            shader = shader,
            parentBoxSize = parentBoxSize.value,
            otherViewPosition = firstButtonPosition.value,
            myPosition = secondButtonPosition.value,
        )
    }
}

@Composable
fun ShadedButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    shader: RuntimeShader,
    parentBoxSize: IntSize,
    myPosition: Offset,
    otherViewPosition: Offset,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    boxSize = it
                }
                .graphicsLayer {
                    shader.setFloatUniform(
                        "resolution",
                        boxSize.width.toFloat(),
                        boxSize.width.toFloat()
                    )
                    println("HUI set parentBoxSize $parentBoxSize")
                    shader.setFloatUniform(
                        "parentBoxSize",
                        parentBoxSize.width.toFloat(),
                        parentBoxSize.height.toFloat()
                    )
                    println("HUI set otherViewPosition $otherViewPosition")
                    shader.setFloatUniform(
                        "otherViewPosition",
                        otherViewPosition.x,
                        otherViewPosition.y
                    )
                    shader.setFloatUniform(
                        "positionInParent",
                        myPosition.x,
                        myPosition.y
                    )
                    this.renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, "image")
                        .asComposeRenderEffect()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(color = Color(0xFFF6F6F6))
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = "Expand Menu",
            tint = Color.Black
        )
    }
}


@Language("AGSL")
private val metaballShader = """
    
    uniform int count;
    uniform float2 otherViewPosition;
    uniform float2 parentBoxSize;
    uniform float2 positionInParent;
    uniform float3 pointColor;
    uniform float percent;
    uniform vec2 resolution;
    uniform shader image;
    
    vec2 NormalizeCoordinates(vec2 o, vec2 r) {
        float2 uv = o / r - 0.5;
        if (r.x >= r.y) {
            uv.x *= r.x / r.y;
        } else {
            uv.y *= r.y / r.x;
        } 
        return uv;
    }
    
    vec4 GetImageTexture(vec2 p, vec2 pivot, vec2 r) {
        if (r.x > r.y) {
            p.x /= r.x / r.y;
        } else {
            p.y /= r.y / r.x;
        }
        p += pivot;
        p *= r;
        return image.eval(p);
    }
    
    float getInfluence(vec2 uv, float ratio) {
        float parentRatio = parentBoxSize.x / resolution.x;
        float2 posInParentNormalized = (positionInParent/parentBoxSize) - 0.5;
        float2 controlPoint = otherViewPosition / parentBoxSize - 0.5;
        controlPoint.x = (controlPoint.x-posInParentNormalized.x) * parentRatio;
       
        float dist = max(1., length(controlPoint-uv) + length(uv));
        float influence = smoothstep(0.,1., 1./pow(dist,2.));
        return influence;
    }
    
    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        
        float parentRatio = parentBoxSize.x / resolution.x;
        float influence = getInfluence(uv, parentRatio);
         
        uv *= 1.-influence;  
        vec4 final = GetImageTexture(uv, vec2(0.5), resolution);
       
        return vec4(final);
     }
""".trimIndent()