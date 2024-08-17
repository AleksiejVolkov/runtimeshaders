package com.offmind.runtimeshaders

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.mask_shader
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuntimeShadersTheme {

                val res = LocalContext.current.resources
                var percentage by remember { mutableFloatStateOf(0f) }

                val bitmap by remember {
                    mutableStateOf(BitmapFactory.decodeResource(res, R.drawable.palm_print))
                }
                val bitmap2 by remember {
                    mutableStateOf(BitmapFactory.decodeResource(res, R.drawable.generic_rainy_city))
                }

                val percentageAnimation = animateFloatAsState(targetValue = percentage, animationSpec = tween(1000),
                    label = ""
                )
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ShaderView(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        percentage = percentageAnimation.value,
                        bitmap = bitmap,
                        bitmap2 = bitmap2,
                        content = {
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                painter = painterResource(id = R.drawable.generic_rainy_city),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    )
                    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp)) {
                      Button(onClick = {
                          percentage = if (percentage == 0f) 1f else 0f
                      }) {
                          Text(text = "Click me")
                      }
                    }
                }
            }
        }
    }
}

@Composable
fun ShaderView(
    modifier: Modifier = Modifier,
    percentage: Float = 0f,
    bitmap: android.graphics.Bitmap,
    bitmap2: android.graphics.Bitmap,
    content: @Composable () -> Unit = {}
) {
    val shader by remember {
        mutableStateOf(RuntimeShader(mask_shader))
    }

    val blurEffect by remember {
        mutableStateOf( RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.DECAL))
    }

    val bitmapShaderMask by remember {
        mutableStateOf(android.graphics.BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
    }

    val bitmapShaderOrigin by remember {
        mutableStateOf(android.graphics.BitmapShader(bitmap2, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR))
    }

    val time by provideTimeAsState(Random.nextFloat() * 10f)

    LaunchedEffect(key1 = bitmapShaderOrigin) {
        shader.setFloatUniform("originSize", bitmap2.width.toFloat(), bitmap2.height.toFloat())
        shader.setFloatUniform("maskSize", bitmap.width.toFloat(), bitmap.height.toFloat())

        shader.setInputBuffer("mask", bitmapShaderMask)
        shader.setInputBuffer("origin", bitmapShaderOrigin)
    }


    Box(modifier = modifier
        .onSizeChanged { size ->
            shader.setFloatUniform(
                "resolution",
                size.width.toFloat(),
                size.height.toFloat()
            )
        }
        .graphicsLayer {
            shader.setFloatUniform("time", time)
            shader.setFloatUniform("percentage", percentage)

            val runtimeShaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "image")
            val chainedEffect = RenderEffect.createChainEffect(runtimeShaderEffect, blurEffect)

            this.renderEffect = chainedEffect.asComposeRenderEffect()

        }) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.1f)))
        {
            content()
        }

    }
}
