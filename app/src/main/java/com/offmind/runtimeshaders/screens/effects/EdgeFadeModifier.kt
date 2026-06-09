package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged

private const val EDGE_FADE_SHADER = """
    uniform shader image;
    uniform vec2 resolution;
    uniform float fade;
    vec4 main(float2 fragCoord) {
        float y = fragCoord.y / resolution.y;
        float alpha = smoothstep(0.0, fade, y) * smoothstep(1.0, 1.0 - fade, y);
        return image.eval(fragCoord) * alpha;
    }
"""

/**
 * Fades the content to transparent along its top and bottom edges, so a scrolling
 * list dissolves into the background instead of being hard-clipped.
 *
 * @param fade fraction of the height consumed by each edge's gradient (0f..0.5f).
 */
fun Modifier.verticalEdgeFade(fade: Float = 0.03f): Modifier = composed {
    val shader = remember { RuntimeShader(EDGE_FADE_SHADER) }
    onSizeChanged { size ->
        shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
    }.graphicsLayer {
        shader.setFloatUniform("fade", fade)
        renderEffect = RenderEffect
            .createRuntimeShaderEffect(shader, "image")
            .asComposeRenderEffect()
    }
}
