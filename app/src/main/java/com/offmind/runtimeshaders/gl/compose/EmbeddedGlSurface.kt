package com.offmind.runtimeshaders.gl.compose

import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.offmind.runtimeshaders.gl.BackgroundCapture
import com.offmind.runtimeshaders.gl.GLRenderer
import com.offmind.runtimeshaders.gl.scene.GlScene

@Composable
fun EmbeddedGlSurface(
    scene: GlScene,
    modifier: Modifier = Modifier,
    backgroundCapture: BackgroundCapture? = null
) {
    AndroidEmbeddedExternalSurface(
        modifier = modifier,
        isOpaque = false
    ) {
        onSurface { surface, width, height ->
            var renderer: GLRenderer? = null
            try {
                renderer = GLRenderer(
                    surface = surface,
                    initialWidth = width,
                    initialHeight = height,
                    scene = scene,
                    backgroundCapture = backgroundCapture
                ).also { renderer ->
                    renderer.start()
                    renderer.resize(width, height)
                }

                surface.onChanged { newWidth, newHeight ->
                    renderer?.resize(newWidth, newHeight)
                }

                surface.onDestroyed {
                    renderer?.release()
                    renderer = null
                }

                while (true) {
                    withFrameNanos { frameTimeNanos ->
                        renderer?.render(frameTimeNanos)
                    }
                }
            } finally {
                renderer?.release()
            }
        }
    }
}
git s