package com.offmind.runtimeshaders.screens.effects

import android.app.Activity
import android.graphics.Bitmap
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.gl.BackgroundCapture
import com.offmind.runtimeshaders.gl.compose.EmbeddedGlSurface
import com.offmind.runtimeshaders.gl.scene.RotatingGlassCubeScene
import kotlinx.coroutines.delay

@Composable
fun TestShaderScreen(paddingValues: PaddingValues) {
    val scene = remember { RotatingGlassCubeScene() }
    val context = LocalContext.current
    val window = remember(context) { context.findActivity().window }
    val backgroundCapture = remember(window) { BackgroundCapture(window) }
    val glassSurfaceSize = 400.dp
    val density = LocalDensity.current
    val graphicsContext = LocalGraphicsContext.current
    val backgroundLayer = remember(graphicsContext) { graphicsContext.createGraphicsLayer() }
    val glassSurfaceSizePx = with(density) { glassSurfaceSize.roundToPx() }
    val glassSurfaceIntSize = remember(glassSurfaceSizePx) {
        IntSize(glassSurfaceSizePx, glassSurfaceSizePx)
    }
    val listState = rememberLazyListState()
    var backgroundVersion by remember { mutableLongStateOf(0L) }

    DisposableEffect(backgroundLayer, graphicsContext) {
        onDispose {
            graphicsContext.releaseGraphicsLayer(backgroundLayer)
        }
    }

    LaunchedEffect(backgroundLayer, backgroundVersion) {
        delay(LAYER_CAPTURE_DEBOUNCE_MS)
        val bitmap = backgroundLayer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        backgroundCapture.submitBitmap(bitmap)
    }

    LaunchedEffect(Unit) {
        delay(INITIAL_LAYER_CAPTURE_DELAY_MS)
        backgroundVersion++
    }

    LaunchedEffect(listState) {
        var lastScrollKey = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        while (true) {
            delay(SCROLL_CAPTURE_INTERVAL_MS)
            val scrollKey = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            if (scrollKey != lastScrollKey || listState.isScrollInProgress) {
                lastScrollKey = scrollKey
                backgroundVersion++
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF920ECB), Color(0xFF212224)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(glassSurfaceSize)
                .clip(RoundedCornerShape(24.dp))
                .drawWithContent {
                    backgroundLayer.record(size = glassSurfaceIntSize) {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(backgroundLayer)
                }
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.78f)
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
            }
        }

        EmbeddedGlSurface(
            scene = scene,
            modifier = Modifier
                .size(glassSurfaceSize),
            backgroundCapture = backgroundCapture
        )
    }
}

private const val LAYER_CAPTURE_DEBOUNCE_MS = 0L
private const val INITIAL_LAYER_CAPTURE_DELAY_MS = 250L
private const val SCROLL_CAPTURE_INTERVAL_MS = 33L

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

private tailrec fun Context.findActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("Activity context is required for PixelCopy window capture.")
    }
}
