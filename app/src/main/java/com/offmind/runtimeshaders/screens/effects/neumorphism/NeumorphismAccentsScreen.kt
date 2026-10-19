package com.offmind.runtimeshaders.screens.effects.neumorphism

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeumorphismAccentsScreen() {
    var dark by remember { mutableStateOf(true) }
    NeuSurface(dark = dark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (dark) "Evening" else "Morning",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = neuColors().text
                    )
                    Text("focus · sound · light · sync", fontSize = 14.sp, color = neuColors().hint)
                }
                // the same toggle as in the thermostat: here it switches the material itself
                NeuToggle(checked = dark, onCheckedChange = { dark = it })
            }
            Spacer(Modifier.height(36.dp))
            FocusTimer()
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                VolumeCard()
                LampCard()
            }
            Spacer(Modifier.height(24.dp))
            DownloadCard()
        }
    }
}

// a ring groove around a concave button with the time in it
@Composable
private fun FocusTimer() {
    // a 25-minute focus session that started 6 minutes ago; one real second per second
    val left = remember { Animatable(19f * 60f) }
    LaunchedEffect(Unit) {
        left.animateTo(0f, tween(durationMillis = (left.value * 1000).toInt(), easing = LinearEasing))
    }
    val seconds = left.value.toInt()
    Box(
        modifier = Modifier
            .size(224.dp)
            .accentTrack(TrackShape.Ring, { left.value / (25f * 60f) }, Focus, thickness = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .neumorphic(cornerRadius = 82.dp, curve = -0.075f, distance = 10.dp, blur = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(seconds / 60, seconds % 60),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    color = neuColors().text
                )
                Text("focus", fontSize = 13.sp, color = neuColors().hint)
            }
        }
    }
}

// a 270° gauge around a concave knob
@Composable
private fun VolumeCard() {
    var volume by remember { mutableFloatStateOf(0.62f) }
    WidgetCard(Modifier.width(196.dp)) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .accentTrack(
                    TrackShape.Ring, { volume }, Sound,
                    thickness = 12.dp, startDeg = 225f, sweepDeg = 270f
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy ->
                        volume = (volume - dy / 900f).coerceIn(0f, 1f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .neumorphic(cornerRadius = 46.dp, curve = -0.075f, distance = 7.dp, blur = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${(volume * 100).toInt()}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                    color = neuColors().text
                )
            }
        }
        CardLabel("Volume")
    }
}

// a tall capsule groove: the dimmer
@Composable
private fun LampCard() {
    var level by remember { mutableFloatStateOf(0.7f) }
    WidgetCard(Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 140.dp)
                .accentTrack(TrackShape.Bar, { level }, Warm, thickness = 44.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy ->
                        level = (level - dy / size.height).coerceIn(0f, 1f)
                    }
                }
        )
        CardLabel("Lamp")
    }
}

// a wide capsule groove: the download
@Composable
private fun DownloadCard() {
    val transition = rememberInfiniteTransition(label = "download")
    val done by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 9000, easing = LinearEasing)),
        label = "done"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(cornerRadius = 24.dp, distance = 8.dp, blur = 18.dp)
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "shaders.zip",
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = neuColors().text
            )
            Text("${(done * 100).toInt()}%", fontSize = 15.sp, color = neuColors().hint)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .accentTrack(TrackShape.Bar, { done }, Sync, thickness = 18.dp)
        )
    }
}

@Composable
private fun WidgetCard(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .height(216.dp)
            .neumorphic(cornerRadius = 28.dp, distance = 8.dp, blur = 18.dp)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }
}

// one accent per widget: where the track starts and where it ends
private val Focus = Color(0xFFFF2D7A) to Color(0xFFA25BFF)
private val Sound = Color(0xFF2FE6FF) to Color(0xFF3D6BFF)
private val Warm = Color(0xFFFF6A2A) to Color(0xFFFFC94A)
private val Sync = Color(0xFF21C7C7) to Color(0xFF52F08A)

@Composable
private fun CardLabel(text: String) {
    Text(text, fontSize = 13.sp, letterSpacing = 1.sp, color = neuColors().hint)
}
