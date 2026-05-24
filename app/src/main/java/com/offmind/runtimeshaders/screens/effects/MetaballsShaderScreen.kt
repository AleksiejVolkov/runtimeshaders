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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language

@Composable
fun MetaballsShaderScreen() {
    var percentage by remember { mutableFloatStateOf(0.0f) }

    val shader = remember {
        Shader(metaballShader).getRuntimeShader()
    }

    Box(
        modifier = Modifier
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

    var boundsPercentValue by remember { mutableFloatStateOf(0f) }
    val boundsPercent by animateFloatAsState(
        targetValue = boundsPercentValue,
        animationSpec = tween(durationMillis = 1500),
    )

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
    var pd by remember { mutableStateOf(0.dp) }
    val pdAnimValue = animateDpAsState(
        targetValue = pd,
        animationSpec = spec.value
    )

    val expandPercentage by remember(pdAnimValue.value) {
        mutableFloatStateOf(pdAnimValue.value.value / 250f)
    }

    LaunchedEffect(expandPercentage) {
        if(expandPercentage == 0f) {
            boundsPercentValue = 0f
        }
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
            shape = CircleShape,
            boundsPercent = 1f,
            onMyCenterReady = { centerX1.floatValue = it }
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
            shape = CircleShape,
            boundsPercent = boundsPercent,
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
            shape = CircleShape,
            boundsPercent = 1f,
            onMyCenterReady = { centerX2.floatValue = it }
        ) {
            expanded = !expanded
            spec.value = if (expanded) expandAnimSpec else collapseAnimSpec
            if (expanded) {
                boundsPercentValue = 1f
            }
             pd = if (expanded) {
                250.dp
            } else {
                0.dp
            }
        }
    }

    if (expanded) {
        BackHandler {
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
    shape: Shape,
    boundsPercent: Float,
    onClick: () -> Unit,
) {
    val myCenter = remember { mutableFloatStateOf(-1f) }

    val currentIcon = remember(expandPercentage) {
        if (expandPercentage > 0.5) {
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
                "boundsPercent" to ShaderTypedValue.FloatType(boundsPercent)
            ),
            includeTime = true,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(shape)
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
                tint = if (expandPercentage > 0.5f) Color.White else Color.Black
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
    uniform float boundsPercent;
    
     float getRawInfluence(vec2 uv, vec2 position, float mass) {
        float dist = max(1.,length(uv)+length(position-uv));
        float rawScale = mass/pow(dist,3.);
        return smoothstep(0.,.5, rawScale);
    }
    
    float getCircle(vec2 uv, vec2 position, float r) {
        return step(length(uv-position),r);
    }
    
    float getInfluence(vec2 uv, vec2 position, float r) {
        float posInParentNormalized = (positionInParent/parentResolution).x - 0.5;
        float2 controlPoint = position / parentResolution - 0.5; //0.5
        controlPoint.x = (controlPoint.x-posInParentNormalized)*r;
        float dist = max(1.,length(uv)+length(controlPoint-uv));
        float rawScale = 1./pow(dist,2.);
        return smoothstep(0.,2., rawScale);
    }
    
    float getInfluenceForUniforms(float2 uv, float r) {
        float influence = 0.0;
        for (int i = 0; i < 10; i++) {
           influence +=  getInfluence(uv, positions[i], r);
           if(i==count-1) break;
        }
        return clamp(influence,0.,1.);
    }
   
    vec3 cosinePalette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
        return a + b * cos(6.28318 * (c * t + d)); // 2π
    }
    
    vec3 getGradient(vec2 uv) {
        vec3 col = cosinePalette(uv.x,
        vec3(0.5),            // базовый уровень
        vec3(0.5),            // амплитуда
        vec3(1.0, 1.0, 1.0),  // частоты каналов (можно 1., 1., 1.)
        vec3(0.00, 0.33, 0.67) // фазовые сдвиги под 3 «тона»
        );
        return col;
    }
   
    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        float r = parentResolution.x/resolution.x;
        
        float posInParentNormalized = (positionInParent/parentResolution).x - 0.5;
        float2 sv = fragCoord / parentResolution;
        sv.x += posInParentNormalized;
    //    sv *= vec2(3.,1.);
     
        float influence = getInfluenceForUniforms(uv, r);
        
        float vertPos = sin(1.6*boundsPercent)*-1.;
        vertPos *= 3.;
        float horizPos = 0.6*cos(5.*boundsPercent);
        float radius = 0.1/abs(vertPos-0.3) * (1.-boundsPercent)*percent;
        vec2 circlePos = vec2(horizPos,vertPos-0.3);
        
        float circle = getCircle(uv, circlePos, radius);
        float inf = getRawInfluence(uv, circlePos, 0.3) * (1.-boundsPercent)*percent;
        influence += inf;
        influence += circle;
        influence = clamp(influence,0.,1.2);
        
        float vertPos2 = sin(1.4*boundsPercent)*-.8;
        vertPos2 *= 1.8;
        float horizPos2 = 0.4*cos(3.*boundsPercent);
        float radius2 = 0.2/abs(vertPos2-0.3) * (1.-boundsPercent)*percent;
        vec2 circlePos2 = vec2(horizPos2,vertPos2-0.3);
        
        float circle2 = getCircle(uv, circlePos2, radius2);
        float inf2 = getRawInfluence(uv, circlePos2, 0.3) * (1.-boundsPercent)*percent;
        influence += inf2;
        influence += circle2;
        influence = clamp(influence,0.,1.2);
        
        float vertPos3 = sin(1.4*boundsPercent)*-.8;
        vertPos3 *= 1.;
        float horizPos3 = 0.2*cos(9.*boundsPercent);
        float radius3 = 0.5/abs(vertPos3-0.5) * (1.-boundsPercent)*percent;
        vec2 circlePos3 = vec2(horizPos3,vertPos3-0.5);
        
        float circle3 = getCircle(uv, circlePos3, radius3);
        float inf3 = getRawInfluence(uv, circlePos3, 0.3) * (1.-boundsPercent)*percent;
        influence += inf3;
        influence += circle3;
        influence = clamp(influence,0.,1.2);
        
        uv *= (1.0 - influence); 
          
        vec4 image = GetImageTexture(uv, vec2(0.5), resolution);
        
        vec3 col = getGradient(sv);    
        
        //col = step(0.6,FBM(sv+time,6)) * vec3(1.0);
        vec3 finalCol = mix(image.rgb,col,percent);
        float alpha = mix(image.a, circle, circle);
        alpha = mix(alpha, circle2, circle2);
        alpha = mix(alpha, circle3, circle3);
        
        
        return vec4(finalCol.rgb*alpha, alpha);
     }
""".trimIndent()
