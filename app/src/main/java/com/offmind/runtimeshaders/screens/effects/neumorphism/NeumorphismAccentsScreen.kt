package com.offmind.runtimeshaders.screens.effects.neumorphism

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeumorphismAccentsScreen() {
    NeuSurface(dark = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text("Evening", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = neuColors().text)
                Text("focus · sound · light · sync", fontSize = 14.sp, color = neuColors().hint)
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
    Box(
        modifier = Modifier
            .size(224.dp)
            .accentTrack(TrackShape.Ring, thickness = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .neumorphic(cornerRadius = 82.dp, curve = -0.075f, distance = 10.dp, blur = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("18:42", fontSize = 40.sp, fontWeight = FontWeight.Light, color = neuColors().text)
                Text("focus", fontSize = 13.sp, color = neuColors().hint)
            }
        }
    }
}

// a 270° gauge around a concave knob
@Composable
private fun VolumeCard() {
    WidgetCard(Modifier.width(196.dp)) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .accentTrack(TrackShape.Ring, thickness = 12.dp, startDeg = 225f, sweepDeg = 270f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .neumorphic(cornerRadius = 46.dp, curve = -0.075f, distance = 7.dp, blur = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("62", fontSize = 26.sp, fontWeight = FontWeight.Light, color = neuColors().text)
            }
        }
        CardLabel("Volume")
    }
}

// a tall capsule groove: the dimmer
@Composable
private fun LampCard() {
    WidgetCard(Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 140.dp)
                .accentTrack(TrackShape.Bar, thickness = 44.dp)
        )
        CardLabel("Lamp")
    }
}

// a wide capsule groove: the download
@Composable
private fun DownloadCard() {
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
            Text("64%", fontSize = 15.sp, color = neuColors().hint)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .accentTrack(TrackShape.Bar, thickness = 18.dp)
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

@Composable
private fun CardLabel(text: String) {
    Text(text, fontSize = 13.sp, letterSpacing = 1.sp, color = neuColors().hint)
}
