package com.offmind.runtimeshaders.screens.effects

import android.graphics.RuntimeShader
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import org.intellij.lang.annotations.Language

@Composable
fun MetaballsShaderScreen(paddingValues: PaddingValues) {
    var percentage by remember { mutableFloatStateOf(0.0f) }
    var expanded by remember { mutableStateOf(false) }

    val shader = remember {
        Shader(metaballShader).getRuntimeShader()
    }

    val percentageAnim = animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(800, easing = FastOutLinearInEasing),
        label = ""
    )

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(Color(0xFF171717)),
        contentAlignment = Alignment.Center
    ) {

        CircleButton(
            shader = shader,
        )

    }
}


@OptIn(ExperimentalAnimationSpecApi::class)
@Composable
fun CircleButton(
    shader: RuntimeShader,
    ) {
    var expanded by remember { mutableStateOf(false) }

    val centerX1 = remember { mutableFloatStateOf(-1f) }
    val centerX2 = remember { mutableFloatStateOf(-1f) }
    val centerX3 = remember { mutableFloatStateOf(-1f) }
    val parentSize = remember { mutableStateOf(IntSize.Zero) }

    val expandAnimSpec = keyframesWithSpline {
        durationMillis = 600
        0.dp at 0
        120.dp atFraction 0.5f
        200.dp atFraction 0.7f
        270.dp atFraction 0.85f
    }

    val collapseAnimSpec = keyframesWithSpline {
        durationMillis = 600
        250.dp at 0
        120.dp atFraction 0.7f
        0.dp atFraction 1f
    }

    var spec = remember { mutableStateOf(expandAnimSpec) }
    var pd by remember {mutableStateOf(150.dp)}
    val pdAnimValue = animateDpAsState(targetValue = pd,
        animationSpec = spec.value)

    val expandPercentage by remember(pdAnimValue.value) {
        mutableFloatStateOf(pdAnimValue.value.value / 250f)
       // mutableFloatStateOf(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                parentSize.value = it.size
            },
        contentAlignment = Alignment.Center
    ) {
        ShadedButton(
            modifier = Modifier.padding(start = pdAnimValue.value.coerceAtLeast(0.dp)),
            shader = shader,
            parentSize = parentSize.value,
            centerOther1 = centerX2.floatValue,
            centerOther2 = centerX3.floatValue,
            expandPercentage = expandPercentage,
            expandedIcon = Icons.Default.Share,
            collapsedIcon = Icons.Default.Share,
            onMyCenterReady = { centerX1.floatValue = it}
        ) {

        }
        ShadedButton(
            modifier = Modifier,
            shader = shader,
            parentSize = parentSize.value,
            centerOther1 = centerX2.floatValue,
            centerOther2 = centerX1.floatValue,
            expandPercentage = expandPercentage,
            expandedIcon = Icons.Default.Favorite,
            collapsedIcon = Icons.Default.Favorite,
            onMyCenterReady = {
                centerX3.floatValue = it
            },
        ) {

        }
        ShadedButton(
            modifier = Modifier.padding(end = pdAnimValue.value.coerceAtLeast(0.dp)),
            shader = shader,
            parentSize = parentSize.value,
            centerOther1 = centerX1.floatValue,
            centerOther2 = centerX3.floatValue,
            expandPercentage = expandPercentage,
            expandedIcon = Icons.Default.Email,
            collapsedIcon = Icons.Default.MoreVert,
            onMyCenterReady = { centerX2.floatValue = it }
        ) {
            expanded = !expanded
            spec.value = if(expanded) expandAnimSpec else collapseAnimSpec
            pd = if (expanded) {
                250.dp
            } else {
                0.dp
            }
        }
    }

    BackHandler {
        if (expanded) {
            spec.value = collapseAnimSpec
            pd = 0.dp
            expanded = false
        }
    }
}

@Composable
fun ShadedButton(
    modifier: Modifier,
    shader: RuntimeShader,
    parentSize: IntSize,
    centerOther1: Float,
    centerOther2: Float,
    expandPercentage: Float,
    expandedIcon: ImageVector,
    collapsedIcon: ImageVector,
    onMyCenterReady: (Float) -> Unit,
    onClick: () -> Unit,
) {
    val myCenter = remember { mutableFloatStateOf(-1f) }

    val currentIcon = remember(expandPercentage) {
        if(expandPercentage > 0.5) {
            mutableStateOf(expandedIcon)
        } else {
            mutableStateOf(collapsedIcon)
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.onGloballyPositioned {
            myCenter.floatValue = it.positionInParent().x + it.size.width / 2f
            onMyCenterReady.invoke(myCenter.floatValue)
        }) {
        ShadedBox(
            shader = shader,
            shaderUniforms = mapOf(
                "count" to ShaderTypedValue.IntType(2),
                "parentResolution" to ShaderTypedValue.Vec2Type(
                    value1 = parentSize.width.toFloat(),
                    value2 = parentSize.height.toFloat(),
                ),
                "positions" to ShaderTypedValue.Vec2Array(
                    listOf(
                        centerOther1 to parentSize.height / 2f,
                        centerOther2 to parentSize.height / 2f,
                        )
                ),
                "pointColor" to ShaderTypedValue.Vec3Type(
                    value1 = 0f,
                    value2 = 0.5f,
                    value3 = 1f,
                ),
                "positionInParent" to ShaderTypedValue.Vec2Type(
                    value1 = myCenter.floatValue,
                    value2 = parentSize.height / 2f
                ),
                "percent" to ShaderTypedValue.FloatType(expandPercentage),
            ),
            includeTime = true,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color = Color(0xFFF6F6F6))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onClick()
                        })
            )
        }
        AnimatedVisibility(
            visible = expandPercentage == 0f || expandPercentage == 1f,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Icon(
                imageVector = currentIcon.value,
                contentDescription = "Expand Menu",
                tint = Color.Black
            )
        }
    }
}

@Language("AGSL")
private val metaballShader = """
    
    uniform int count;
    uniform float2 positions[10];
    uniform float2 parentResolution;
    uniform float2 positionInParent;
    uniform float3 pointColor;
    uniform float percent;
    
    
    // Параметры: две точки a и b в пространстве (vec2 или vec3)
    float stickyWeight(vec2 a, vec2 b, float d) {
        // вычисляем расстояние
        //float d = length(a - b);
        // добавляем маленькое значение, чтобы избежать деления на ноль
        d = max(d, 1e-6);
        // 1 / sqrt(d)
        return inversesqrt(pow(d,5.));
     //   return 1.0 / pow(d + 1.0, 0.8);
     /* float inv2 = 1.0 / (d * d);
      float eps  = pow(2.0, 1.8 - 2.0);
      float tail = eps / pow(d, 1.8);
      return inv2 + tail;*/
    }
    
    float getInfluence(float2 uv) {
        float mass = 0.45;
        float influence = 0.0;
        float r = parentResolution.x/resolution.x;
        for (int i = 0; i < 10; i++) {
            
            float posInParentNormalized = (positionInParent/parentResolution).x - 0.5;
            float2 controlPoint = positions[i] / parentResolution - 0.5; //0.5
            controlPoint.x = (controlPoint.x-posInParentNormalized)*r;
            float dist = max(1.,length(uv)+length(controlPoint-uv));
            //float rawScale = (mass)/pow(0.75*dist,1.);
            float rawScale = stickyWeight(uv,controlPoint, dist);
            influence += smoothstep(0.,1., rawScale);
            
            if(i==count-1) break;
        }
        return clamp(influence,0.,1.);
    }
    
    float remap(float value, float inMin, float inMax, float outMin, float outMax) {
       return ((value - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin;
    }
    
    
    vec3 getGradient(vec2 uv, vec3 color) {
       // scale UV to grid
        float freq = 20.0;
        vec2 gv = uv * freq;
        
        // create horizontal + vertical wave masks
        float w1 = abs(fract(gv.x) - 0.5);
        float w2 = abs(fract(gv.y) - 0.5);
        float mask = smoothstep(0.45, 0.48, min(w1, w2));
        
        // pulsate mask over time
        float pulse = 0.5 + 0.5 * sin(time * 3.0 + gv.x + gv.y);
        mask *= pulse;
        
        // base gradient
        vec3 bg = mix(color, color*vec3(0.2), uv.y);
        
        // glow the lines
        vec3 lineColor = vec3(1.0);
        return mix(bg, lineColor, mask);
    }
    
    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        float r = parentResolution.x/resolution.x;
        
        float posInParentNormalized = (positionInParent/parentResolution).x - 0.5;
        float2 sv = fragCoord / parentResolution;
        sv.x += posInParentNormalized;
      
        float radius = 0.1;
        float mass = .5;
        
        float influence = getInfluence(uv); // из массива позиций
        uv *= (1.0 - influence); 
       
        
        vec4 image = GetImageTexture(uv, vec2(0.5), resolution);
        
        vec3 col = getGradient(sv,image.rgb);    
        
        vec3 finalCol = mix(image.rgb,col,0.);
        return vec4(image);
     }
""".trimIndent()

