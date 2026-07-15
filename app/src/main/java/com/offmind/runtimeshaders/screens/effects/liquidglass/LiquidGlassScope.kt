package com.offmind.runtimeshaders.screens.effects.liquidglass

import android.graphics.RenderEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform
import kotlin.math.roundToInt

/**
 * Establishes a liquid-glass scope. The [content] lambda receives a [LiquidGlassScopeReceiver],
 * making [LiquidGlassScopeReceiver.glassBackdrop] and [LiquidGlassScopeReceiver.liquidGlass]
 * available as plain modifier calls — same pattern as
 * [com.offmind.runtimeshaders.screens.effects.lighting.LightingScope].
 *
 * How it works (and why it's cheap): the backdrop subtree is recorded into a shared
 * [GraphicsLayer]. A GraphicsLayer is a display list backed by a RenderNode — recording and
 * replaying it is GPU-side work with **no bitmap copies and no pixel readback**. Each glass
 * element replays the slice of that layer behind its own bounds into its own GraphicsLayer,
 * whose render effect chains a hardware blur with the AGSL liquid-glass shader. Because the
 * replay references live RenderNodes, scrolling or animating backdrop content updates the
 * glass without recomposition.
 */
@Composable
fun LiquidGlassScope(
    modifier: Modifier = Modifier,
    content: @Composable LiquidGlassScopeReceiver.() -> Unit,
) {
    val backdropLayer = rememberGraphicsLayer()
    val state = remember { LiquidGlassState() }
    state.backdropLayer = backdropLayer
    val receiver = remember(state) { LiquidGlassScopeReceiverImpl(state) }

    Box(modifier = modifier) {
        receiver.content()
    }
}

// ── Internal implementation ───────────────────────────────────────────────────

internal class LiquidGlassState {
    /** Shared display list holding the backdrop subtree's draw commands. */
    var backdropLayer: GraphicsLayer? = null

    /** Backdrop origin in root coordinates; glass elements offset their replay by this. */
    var sourceOrigin: Offset by mutableStateOf(Offset.Zero)
}

private class LiquidGlassScopeReceiverImpl(
    private val state: LiquidGlassState,
) : LiquidGlassScopeReceiver {

    override fun Modifier.glassBackdrop(): Modifier = this
        .onGloballyPositioned { coords ->
            state.sourceOrigin = coords.positionInRoot()
        }
        .drawWithContent {
            val layer = state.backdropLayer
            if (layer != null) {
                layer.record(
                    size = IntSize(size.width.roundToInt(), size.height.roundToInt())
                ) {
                    this@drawWithContent.drawContent()
                }
                drawLayer(layer)
            } else {
                drawContent()
            }
        }

    override fun Modifier.liquidGlass(
        cornerRadius: Dp,
        blurRadius: Dp,
        refractionHeight: Dp,
        refractionAmount: Dp,
        tint: Color,
        vibrancy: Float,
    ): Modifier = composed {
        val glassLayer = rememberGraphicsLayer()
        val shader = remember {
            Shader(liquidGlassShaderSource).getRuntimeShader(
                uniforms = listOf(
                    Uniform(Uniform.Type.SHADER, "image"),
                    Uniform(Uniform.Type.VEC2, "resolution"),
                    Uniform(Uniform.Type.FLOAT, "cornerRadius"),
                    Uniform(Uniform.Type.FLOAT, "refractionHeight"),
                    Uniform(Uniform.Type.FLOAT, "refractionAmount"),
                    Uniform(Uniform.Type.VEC4, "tint"),
                    Uniform(Uniform.Type.FLOAT, "vibrancy"),
                ),
                customFunctions = emptySet(),
            )
        }
        var position by remember { mutableStateOf(Offset.Zero) }

        this
            .onGloballyPositioned { coords ->
                position = coords.positionInRoot()
            }
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                val backdrop = state.backdropLayer ?: return@drawBehind
                val relative = position - state.sourceOrigin

                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("cornerRadius", cornerRadius.toPx())
                shader.setFloatUniform("refractionHeight", refractionHeight.toPx())
                shader.setFloatUniform("refractionAmount", refractionAmount.toPx())
                shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
                shader.setFloatUniform("vibrancy", vibrancy)

                glassLayer.record(
                    size = IntSize(size.width.roundToInt(), size.height.roundToInt())
                ) {
                    translate(left = -relative.x, top = -relative.y) {
                        drawLayer(backdrop)
                    }
                }

                val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "image")
                val blurPx = blurRadius.toPx()
                val effect = if (blurPx > 0f) {
                    RenderEffect.createChainEffect(
                        shaderEffect,
                        RenderEffect.createBlurEffect(
                            blurPx,
                            blurPx,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    )
                } else {
                    shaderEffect
                }
                glassLayer.renderEffect = effect.asComposeRenderEffect()

                drawLayer(glassLayer)
            }
    }
}
