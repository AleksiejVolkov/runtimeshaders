package com.offmind.runtimeshaders.screens.effects.glassrefraction

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.intellij.lang.annotations.Language

// The glass is drawn by the backdrop, not by the panel: this layer already has every pixel
// the glass needs to bend.
fun Modifier.glassBackdrop(): Modifier = composed {
    val identity = remember { RuntimeShader(identityShader) }
    graphicsLayer {
        renderEffect = RenderEffect.createRuntimeShaderEffect(identity, "image")
            .asComposeRenderEffect()
    }
}

// Changes nothing: the backdrop goes through a shader and comes out as it was.
@Language("AGSL")
private val identityShader = """
    uniform shader image;

    half4 main(float2 xy) {
        return image.eval(xy);
    }
"""
