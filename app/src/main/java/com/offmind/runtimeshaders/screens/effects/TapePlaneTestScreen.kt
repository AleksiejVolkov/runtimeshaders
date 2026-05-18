package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TapePlaneTestScreen(paddingValues: PaddingValues) {
    val scene = remember { TapePlaneScene() }
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    var cameraControls by remember { mutableStateOf(scene.cameraControls()) }
    var curvePointControls by remember { mutableStateOf(scene.curvePointControls()) }
    var sliderValue by remember { mutableStateOf(0f) }

    fun refreshControls() {
        cameraControls = scene.cameraControls()
        curvePointControls = scene.curvePointControls()
    }

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
            painter = painterResource(id = R.drawable.generic_mountians),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.32f,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp)) {
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = (value / SLIDER_STEP).roundToInt() * SLIDER_STEP
                    scene.setMorphProgress(sliderValue)
                    refreshControls()
                },
                valueRange = 0f..1f,
                steps = SLIDER_STEPS,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
        Box(
            modifier = Modifier
                .padding(bottom = 100.dp)
                .size(width = 340.dp, height = 35.dp).background(color = Color.DarkGray, shape = RoundedCornerShape(16.dp))
        )
        EmbeddedGlSurface(
            scene = scene,
            modifier = Modifier.size(width = 340.dp, height = 300.dp).padding(bottom = 100.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navigationBarsPadding.calculateBottomPadding() + 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ValueText(
                text = "camera yaw=${cameraControls.yawRadians.format()} elev=${cameraControls.elevationRadians.format()} dist=${cameraControls.distance.format()}"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    label = "-",
                    onClick = {
                        scene.changeCameraDistance(CAMERA_DISTANCE_STEP)
                        refreshControls()
                    }
                )
                OrbitPad(
                    scene = scene,
                    onOrbitChanged = ::refreshControls
                )
                ControlButton(
                    label = "+",
                    onClick = {
                        scene.changeCameraDistance(-CAMERA_DISTANCE_STEP)
                        refreshControls()
                    }
                )
            }
            CurvePointControlsPanel(
                points = curvePointControls,
                onMovePoint = { pointIndex, deltaY, deltaZ ->
                    scene.moveCurvePoint(
                        index = pointIndex - 1,
                        deltaY = deltaY,
                        deltaZ = deltaZ
                    )
                    refreshControls()
                },
                onChangeWidth = { pointIndex, delta ->
                    scene.changeCurvePointWidth(
                        index = pointIndex - 1,
                        delta = delta
                    )
                    refreshControls()
                }
            )
        }
    }
}

@Composable
private fun OrbitPad(
    scene: TapePlaneScene,
    onOrbitChanged: () -> Unit
) {
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
                    onOrbitChanged()
                }
            }
    )
}

@Composable
private fun CurvePointControlsPanel(
    points: List<TapePlaneScene.CurvePointControls>,
    onMovePoint: (pointIndex: Int, deltaY: Float, deltaZ: Float) -> Unit,
    onChangeWidth: (pointIndex: Int, delta: Float) -> Unit
) {
    Column(
        modifier = Modifier
            .width(350.dp)
            .background(Color(0x99071018), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        points.forEach { point ->
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "P${point.index}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    SmallControlButton(label = "Up") {
                        onMovePoint(point.index, 0f, POINT_HEIGHT_STEP)
                    }
                    SmallControlButton(label = "Down") {
                        onMovePoint(point.index, 0f, -POINT_HEIGHT_STEP)
                    }
                    SmallControlButton(label = "Left") {
                        onMovePoint(point.index, -POINT_Y_STEP, 0f)
                    }
                    SmallControlButton(label = "Right") {
                        onMovePoint(point.index, POINT_Y_STEP, 0f)
                    }
                    SmallControlButton(label = "Thin") {
                        onChangeWidth(point.index, -POINT_WIDTH_STEP)
                    }
                    SmallControlButton(label = "Wide") {
                        onChangeWidth(point.index, POINT_WIDTH_STEP)
                    }
                }
                Text(
                    text = "x=${point.x.format()} y=${point.y.format()} z=${point.z.format()} w=${point.width.format()}",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                )
            }
        }
    }
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

@Composable
private fun SmallControlButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC102438))
            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ValueText(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.84f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(Color(0x99071018), RoundedCornerShape(6.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

private fun Float.format(): String {
    return String.format(Locale.US, "%.2f", this)
}

private const val ORBIT_RADIANS_PER_PIXEL = 0.008f
private const val CAMERA_DISTANCE_STEP = 0.35f
private const val POINT_HEIGHT_STEP = 0.18f
private const val POINT_Y_STEP = 0.18f
private const val POINT_WIDTH_STEP = 0.12f
private const val SLIDER_STEP = 0.05f
private const val SLIDER_STEPS = 19
