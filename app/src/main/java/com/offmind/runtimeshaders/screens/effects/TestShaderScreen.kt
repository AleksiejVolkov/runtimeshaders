package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.gl.compose.CapturedBackgroundGlBox
import com.offmind.runtimeshaders.gl.scene.RotatingGlassCubeScene

@Composable
fun TestShaderScreen() {
    val scene = remember { RotatingGlassCubeScene() }
    val glassSurfaceSize = 400.dp
    val listState = rememberLazyListState()
    var captureVersion by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect {
            captureVersion++
        }
    }

    val listItems = remember {
        List(40) { index ->
            GlassListItem(
                title = "Glass list item ${index + 1}",
                category = categories[index % categories.size],
                status = statuses[index % statuses.size],
                description = descriptions[index % descriptions.size],
                accent = accentColors[index % accentColors.size]
            )
        }
    }

    CapturedBackgroundGlBox(
        scene = scene,
        surfaceSize = DpSize(glassSurfaceSize, glassSurfaceSize),
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF920ECB), Color(0xFF212224)))),
        surfaceAlignment = Alignment.Center,
        surfaceModifier = Modifier
            .size(glassSurfaceSize),
        captureVersion = captureVersion
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4ABF79),
                            Color(0xFF8054D5),
                            Color(0xFF2657BA),
                            Color(0xFF082461),
                        )
                    )
                )
                .padding(14.dp)
        ) {
            item {
                Text(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = "Captured LazyColumn texture",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(listItems) { item ->
                CardItem(item)
            }
        }
    }
}

@Composable
private fun CardItem(item: GlassListItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xC3E0E0E0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(item.accent, RoundedCornerShape(50))
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.title,
                    color = Color(0xFF10213D),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = item.status,
                    color = Color(0xFF38506F),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = item.category,
                color = item.accent,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = item.description,
                color = Color(0xFF40506A),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class GlassListItem(
    val title: String,
    val category: String,
    val status: String,
    val description: String,
    val accent: Color
)

private val categories = listOf("Shader pass", "Compose layer", "Texture sync", "Gesture state")
private val statuses = listOf("Live", "Queued", "Stable", "Preview")
private val descriptions = listOf(
    "Dynamic row with enough contrast to reveal bending through the glass volume.",
    "Captured from the Compose subtree and uploaded into the GL material.",
    "Scroll this list to verify the texture refresh cadence during movement.",
    "Mixed text, badges, and color chips make distortion easier to inspect."
)
private val accentColors = listOf(
    Color(0xFF00C2A8),
    Color(0xFFFFC857),
    Color(0xFF9B7BFF),
    Color(0xFFFF6B9A)
)
