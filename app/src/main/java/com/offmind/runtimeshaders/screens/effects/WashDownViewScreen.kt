package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.LoginForm
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.washDownToDisappear
import kotlinx.coroutines.delay

@Composable
fun WashDownViewScreen() {
    var percentage by remember { mutableFloatStateOf(0f) }
    var success by remember { mutableStateOf(false) }

    LaunchedEffect(success) {
        if (success) {
            percentage = 0f
            while (percentage < 1f) {
                percentage += 0.01f
                delay(10)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val shader = remember {
            Shader(washDownToDisappear).getRuntimeShader()
        }
        Image(
            painter = painterResource(id = R.drawable.generic_mountians),
            contentDescription = "Sample Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        ShadedBox(
            shader = shader,
            shaderUniforms = mapOf(
                "time" to ShaderTypedValue.FloatType(provideTimeAsState().value),
                "percentage" to ShaderTypedValue.FloatType(percentage)
            )
        ) {
            LoginForm {
                success = it
            }
        }
    }
}