package com.offmind.runtimeshaders.screens.effects.pixeldisplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Number of grid cells across the display. Higher = finer resolution.
 */
private const val GridColumns = 220f

/** Everything the panel draws is light-on-dark so it reads cleanly through the dot-matrix shader. */
private val PanelInk = Color.White

/**
 * A "pixel display" screen: ordinary Material3 components rendered through a dot-matrix shader.
 *
 * Whatever you drop inside the [ShadedBox] is quantised into a grid of dots (empty) and white
 * squares (lit), so standard buttons / text fields / switches automatically take on the LED-panel
 * look. Tune the chunkiness via [GridColumns].
 */
@Composable
fun PixelDisplayScreen() {
    val shader = remember {
        Shader(pixelDisplayShaderSource).getRuntimeShader(
            uniforms = listOf(
                Uniform(Uniform.Type.SHADER, "image"),
                Uniform(Uniform.Type.VEC2, "resolution"),
                Uniform(Uniform.Type.FLOAT, "gridColumns"),
            ),
            customFunctions = emptySet(),
        )
    }

    ShadedBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        shader = shader,
        shaderUniforms = mapOf("gridColumns" to ShaderTypedValue.FloatType(GridColumns)),
    ) {
        PixelDisplayContent()
    }
}

@Composable
private fun PixelDisplayContent() {
    var soundOn by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("HELLO") }

    // Live clock so the pixels visibly re-render every second.
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var clock by remember { mutableStateOf(timeFormat.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = timeFormat.format(Date())
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeContentPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Hero clock — live system time so the pixel digits update every second.
        Text(
            text = clock,
            color = PanelInk,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "PIXEL UI",
            color = PanelInk,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp,
        )

        Spacer(Modifier.height(4.dp))

        // Filled button -> solid block of pixels with cut-out label.
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PanelInk,
                contentColor = Color.Black,
            ),
        ) {
            Text("START", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Outlined button -> pixel outline + pixel label.
        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PanelInk),
            border = BorderStroke(3.dp, PanelInk),
        ) {
            Text("SETTINGS", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Text field -> editable pixel text inside a pixel frame.
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PanelInk,
                unfocusedTextColor = PanelInk,
                cursorColor = PanelInk,
                focusedBorderColor = PanelInk,
                unfocusedBorderColor = PanelInk,
            ),
        )

        // Switch + label -> pixel toggle.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SOUND", color = PanelInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(20.dp))
            Switch(
                checked = soundOn,
                onCheckedChange = { soundOn = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = PanelInk,
                    checkedBorderColor = PanelInk,
                    uncheckedThumbColor = PanelInk,
                    uncheckedTrackColor = Color.Black,
                    uncheckedBorderColor = PanelInk,
                ),
            )
        }

        Image(
            painter = painterResource(id = R.drawable.generic_image),
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Pixel Display",
        )
    }
}
