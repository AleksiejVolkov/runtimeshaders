package com.offmind.runtimeshaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.generated.ShaderDependencyMap
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.chromaticAberrationEffect
import com.offmind.runtimeshaders.shaders.shockwave
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var pointerPos by remember { mutableStateOf(Offset.Zero) }
            var percentage by remember { mutableFloatStateOf(1f) }
            var animate by remember { mutableStateOf(false) }

            LaunchedEffect(animate) {
                if (animate) {
                    percentage = 0f
                    while (percentage < 1f) {
                        percentage += 0.01f
                        delay(3)
                    }
                    animate = false
                }
            }

            RuntimeShadersTheme {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.generic_mountians),
                        contentDescription = "Sample Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    val shader = remember { Shader(chromaticAberrationEffect).getRuntimeShader() }
                    ShadedBox(
                        modifier = Modifier.fillMaxSize(),
                        shader = shader,
                        shaderUniforms = mapOf(
                            "time" to ShaderTypedValue.FloatType(provideTimeAsState().value),
                        )
                    ) {
                        SampleContent()
                    }
                }
            }
        }
    }
}

@Composable
fun SampleContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .padding(16.dp)
                .background(Color.White, CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.launcher_fg),
                contentDescription = "Sample Image",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color.DarkGray),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

