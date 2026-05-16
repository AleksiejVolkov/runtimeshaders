package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.gl.compose.EmbeddedGlSurface
import com.offmind.runtimeshaders.gl.scene.RotatingCubeScene

@Composable
fun TestShaderScreen(paddingValues: PaddingValues) {
    val scene = remember { RotatingCubeScene() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFBDBDBD), Color(0xFFE5E5E5))))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 30.dp)
                .background(Color(0xFF56635B), shape = RoundedCornerShape(16.dp))
                .safeContentPadding(),
        ) {
            Text(
                text = "Compose text behind transparent GL surface",
                color = Color(0xFFEAEAEA),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Box(modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = R.drawable.logo_label), contentDescription = null)
            }
            Button(onClick = { }) {
                 Text(text = "Start")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        EmbeddedGlSurface(
            scene = scene,
            modifier = Modifier.fillMaxSize()
        )
    }
}
