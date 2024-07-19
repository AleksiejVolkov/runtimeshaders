package com.offmind.runtimeshaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.health.connect.datatypes.units.Percentage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.text_from_noise
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuntimeShadersTheme {
                Box(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    var percentage by remember {
                        mutableFloatStateOf(0.0f)
                    }

                    val percentageAnim = animateFloatAsState(targetValue = percentage, label = "")

                    ShaderView(
                        modifier = Modifier
                            .clipToBounds()
                            .clickable {
                                percentage = if (percentage == 0.0f) 1.0f else 0.0f
                            },
                        content = {
                            Text(text = "Hello, World!", color = Color.Black, fontSize = 24.sp)
                        },
                        percentage = percentageAnim.value
                    )
                }
            }
        }
    }
}

@Composable
fun ShaderView(
    modifier: Modifier = Modifier,
    percentage: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val shader by remember {
        mutableStateOf(RuntimeShader(text_from_noise))
    }
    val time by provideTimeAsState(Random.nextFloat() * 10f)

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
            this.renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "image")
                .asComposeRenderEffect()
        }) {
        content()
    }
}
