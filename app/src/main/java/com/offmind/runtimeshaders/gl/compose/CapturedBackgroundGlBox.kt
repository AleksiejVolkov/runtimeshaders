package com.offmind.runtimeshaders.gl.compose

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import com.offmind.runtimeshaders.gl.BackgroundCapture
import com.offmind.runtimeshaders.gl.scene.GlScene
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CapturedBackgroundGlBox(
    scene: GlScene,
    surfaceSize: DpSize,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    surfaceAlignment: Alignment = Alignment.Center,
    captureVersion: Long = 0L,
    captureIntervalMillis: Long = DEFAULT_CAPTURE_INTERVAL_MS,
    backgroundContent: @Composable BoxScope.() -> Unit
) {
    val graphicsContext = LocalGraphicsContext.current
    val density = LocalDensity.current
    val backgroundLayer = remember(graphicsContext) { graphicsContext.createGraphicsLayer() }
    val backgroundCapture = remember { BackgroundCapture() }
    val surfaceSizePx = remember(surfaceSize, density) {
        with(density) {
            IntSize(surfaceSize.width.roundToPx(), surfaceSize.height.roundToPx())
        }
    }

    var surfaceOffset by remember { mutableStateOf(Offset.Zero) }
    var internalCaptureVersion by remember { mutableLongStateOf(0L) }
    var drawVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(backgroundLayer, backgroundCapture, graphicsContext) {
        onDispose {
            backgroundCapture.release()
            graphicsContext.releaseGraphicsLayer(backgroundLayer)
        }
    }

    LaunchedEffect(captureVersion, surfaceOffset, surfaceSizePx) {
        delay(INITIAL_CAPTURE_DELAY_MS.milliseconds)
        internalCaptureVersion++
    }

    LaunchedEffect(captureIntervalMillis) {
        if (captureIntervalMillis <= 0L) return@LaunchedEffect
        while (true) {
            delay(captureIntervalMillis.milliseconds)
            internalCaptureVersion++
        }
    }

    LaunchedEffect(backgroundLayer, internalCaptureVersion, drawVersion) {
        if (drawVersion == 0) return@LaunchedEffect
        val bitmap = backgroundLayer
            .toImageBitmap()
            .asAndroidBitmap()
            .copy(Bitmap.Config.ARGB_8888, false)
        if (bitmap.hasVisibleContent()) {
            backgroundCapture.submitBitmap(bitmap)
        } else {
            bitmap.recycle()
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    backgroundLayer.record(size = surfaceSizePx) {
                        translate(
                            left = -surfaceOffset.x.roundToInt().toFloat(),
                            top = -surfaceOffset.y.roundToInt().toFloat()
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    drawVersion++
                    drawContent()
                }
        ) {
            backgroundContent()
        }

        EmbeddedGlSurface(
            scene = scene,
            modifier = Modifier
                .align(surfaceAlignment)
                .then(surfaceModifier)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInParent()
                    surfaceOffset = bounds.topLeft
                },
            backgroundCapture = backgroundCapture
        )
    }
}

private fun Bitmap.hasVisibleContent(): Boolean {
    if (width <= 0 || height <= 0) return false

    val sampleXs = intArrayOf(width / 4, width / 2, width * 3 / 4)
    val sampleYs = intArrayOf(height / 4, height / 2, height * 3 / 4)
    var visibleSamples = 0

    sampleXs.forEach { x ->
        sampleYs.forEach { y ->
            val color = getPixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
            val alpha = color ushr 24
            val red = color shr 16 and 0xFF
            val green = color shr 8 and 0xFF
            val blue = color and 0xFF
            if (alpha > 8 && red + green + blue > 24) {
                visibleSamples++
            }
        }
    }

    return visibleSamples >= MIN_VISIBLE_SAMPLES
}

private const val DEFAULT_CAPTURE_INTERVAL_MS = 0L
private const val INITIAL_CAPTURE_DELAY_MS = 120L
private const val MIN_VISIBLE_SAMPLES = 3
