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
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.generated.ShaderDependencyMap
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
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
                    ShaderView(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        percentage = percentage,
                        content = {
                            SampleContent { success ->
                                animate = !success
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ShaderView(
    modifier: Modifier = Modifier,
    pointerPos: Offset = Offset.Zero,
    percentage: Float = 0.5f,
    content: @Composable () -> Unit = {}
) {
    val shader by remember {
        mutableStateOf(
            Shader(shockwave).getRuntimeShader(
                basicUniformList
                    .addUniform(Uniform.Type.VEC2 to "pointer")
            )
        )
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
            shader.setFloatUniform("pointer", pointerPos.x, pointerPos.y)
            shader.setFloatUniform("percentage", percentage)
            this.renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "image")
                .asComposeRenderEffect()
        }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.1f))
        )
        {
            content()
        }
    }
}

@Composable
fun SampleContent(
    onLogin: (success: Boolean) -> Unit = {}
) {
    var visib by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.generic_mountians),
            contentDescription = "Sample Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(shape = CircleShape, color = Color.White)
                .clickable { onLogin(false) },
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Image(
                painter = painterResource(id = R.drawable.launcher_fg),
                contentDescription = "Sample Image",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color.DarkGray),
                modifier = Modifier.size(200.dp)
            )
            /*AnimatedVisibility(!visib) {
                LoginForm { success ->
                    visib = success
                 //   onLogin(success)
                }
            }*/
        }
    }
}

@Composable
fun LoginForm(
    onLogin: (success: Boolean) -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var error: String? by remember { mutableStateOf(null) }
    var success: Boolean by remember { mutableStateOf(false) }
    var loading: Boolean by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(0) }

    LaunchedEffect(loading) {
        if (loading) {
            delay(2300)
            onLogin(steps == 3)
            loading = false
            if (steps == 3) {
                success = true
            } else {
                error = "Invalid username or password"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Login", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(text = "Username") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            Button(
                onClick = {
                    steps++
                    loading = true
                    error = null

                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )
                } else
                    Text(text = "Login")
            }
        }
    }
}
