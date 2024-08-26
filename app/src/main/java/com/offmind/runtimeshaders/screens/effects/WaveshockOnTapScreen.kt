package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.LoginForm
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.shockwave
import kotlinx.coroutines.delay

@Composable
fun WaveshockOnTapScreen() {
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var percentage by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pointerPos)
    {
        percentage = 0f
        while (percentage < 1f) {
            percentage += 0.01f
            delay(5)
        }
    }

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .pointerInput(Unit)
            {
                while (true) {
                    val position = this.awaitPointerEventScope {
                        awaitFirstDown().position
                    }
                    pointerPos = position
                }
            },
        contentAlignment = Alignment.Center
    )
    {
        val shader = remember {
            Shader(shockwave).getRuntimeShader(
                uniforms =
                basicUniformList.addUniform(Uniform.Type.VEC2 to "pointer"),
            )
        }
        ShadedBox(
            modifier = Modifier.fillMaxSize(),
            shader = shader,
            shaderUniforms = mapOf(
                "time" to ShaderTypedValue.FloatType(provideTimeAsState().value),
                "pointer" to ShaderTypedValue.Vec2Type(pointerPos.x, pointerPos.y),
                "percentage" to ShaderTypedValue.FloatType(percentage)
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.generic_mountians),
                contentDescription = "Sample Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            LoginForm()
        }
    }
}