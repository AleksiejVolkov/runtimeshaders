package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.gl.compose.EmbeddedGlSurface
import com.offmind.runtimeshaders.gl.scene.TapePlaneScene
import androidx.compose.foundation.clickable

@Composable
fun TapePlaneTestScreen(paddingValues: PaddingValues) {
    val scene = remember { TapePlaneScene() }
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF15324A),
                        Color(0xFF2F6472),
                        Color(0xFF111820)
                    )
                )
            )
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.generic_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.32f,
            modifier = Modifier.fillMaxSize()
        )
        EmbeddedGlSurface(
            scene = scene,
            modifier = Modifier.size(width = 340.dp, height = 120.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navigationBarsPadding.calculateBottomPadding() + 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    label = "-",
                    onClick = { scene.changeCameraDistance(CAMERA_DISTANCE_STEP) }
                )
                OrbitPad(scene = scene)
                ControlButton(
                    label = "+",
                    onClick = { scene.changeCameraDistance(-CAMERA_DISTANCE_STEP) }
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlButton(
                    label = "Up",
                    onClick = { scene.movePoint2Height(POINT_HEIGHT_STEP) }
                )
                ControlButton(
                    label = "Down",
                    onClick = { scene.movePoint2Height(-POINT_HEIGHT_STEP) }
                )
            }
        }
    }
}

@Composable
private fun OrbitPad(scene: TapePlaneScene) {
    Box(
        modifier = Modifier
            .size(width = 210.dp, height = 96.dp)
            .background(Color(0xAA071018), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .pointerInput(scene) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    scene.orbit(
                        deltaYawRadians = dragAmount.x * ORBIT_RADIANS_PER_PIXEL,
                        deltaElevationRadians = -dragAmount.y * ORBIT_RADIANS_PER_PIXEL
                    )
                }
            }
    )
}

@Composable
private fun ControlButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC102438))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private const val ORBIT_RADIANS_PER_PIXEL = 0.008f
private const val CAMERA_DISTANCE_STEP = 0.35f
private const val POINT_HEIGHT_STEP = 0.18f
