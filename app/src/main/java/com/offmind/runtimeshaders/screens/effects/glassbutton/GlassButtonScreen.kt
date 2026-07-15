package com.offmind.runtimeshaders.screens.effects.glassbutton

import android.graphics.RenderEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.composables.provideTimeAsState
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform

private object GlassPalette {
    val SurfaceTop = Color(0xFFF6F6F3)
    val SurfaceCenter = Color(0xFFE6E7E5)
    val SurfaceBottom = Color(0xFFD0D2D2)
    val SurfaceShade = Color(0xFF9EA8AE)
    val WarmGlow = Color(0xFFFFD7B2)
    val LightTint = Color(0xFFFFFFFF)
    val Text = Color(0xFF111418)
    val SecondaryText = Color(0xFF252A30)
    val Placeholder = Color(0xFF73777D)
    val GlassTop = Color.White.copy(alpha = 0.40f)
    val GlassBottom = Color(0xFFEAF1F3).copy(alpha = 0.20f)
    val Hairline = Color.White.copy(alpha = 0.46f)
    val CoolLine = Color(0xFF75848A).copy(alpha = 0.42f)
    val ShadowAmbient = Color(0xFF64727A).copy(alpha = 0.18f)
    val ShadowSpot = Color(0xFF58656D).copy(alpha = 0.30f)
}

@Composable
fun GlassButtonScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassPalette.SurfaceTop,
                        GlassPalette.SurfaceCenter,
                        GlassPalette.SurfaceBottom
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlassPalette.WarmGlow.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(760f, 1020f),
                        radius = 560f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlassPalette.SurfaceShade.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(120f, 1280f),
                        radius = 720f
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
        ) {
            val compact = maxWidth < 430.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 20.dp else 34.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Understanding the Glass",
                    color = GlassPalette.Text,
                    fontSize = if (compact) 26.sp else 30.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "SDF normals, one light source, soft contact shadows",
                    color = GlassPalette.SecondaryText.copy(alpha = 0.70f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(if (compact) 34.dp else 46.dp))

                GlassShowcaseGrid(
                    modifier = Modifier.widthIn(max = 620.dp),
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun GlassShowcaseGrid(
    modifier: Modifier,
    compact: Boolean
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 24.dp else 30.dp)
    ) {
        GlassShowcaseRow {
            GlassExample(label = "Start project") {
                GlassPill(text = "Start project")
            }
            GlassExample(label = "Secondary") {
                GlassPill(text = "Secondary")
            }
        }
        GlassShowcaseRow {
            GlassExample(label = "Icon button") {
                GlassIconButton()
            }
            GlassExample(label = "Select") {
                GlassSelect(text = "Select")
            }
        }
        GlassShowcaseRow {
            GlassExample(label = "Text field") {
                GlassTextField(text = "Text field")
            }
            GlassExample(label = "Tabs") {
                GlassTabs()
            }
        }
        GlassShowcaseRow {
            GlassExample(label = "Switch") {
                GlassSwitch()
            }
            GlassExample(label = "Pro plan") {
                GlassPill(text = "Pro plan", compact = true)
            }
        }
    }
}

@Composable
private fun GlassShowcaseRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        content()
    }
}

@Composable
private fun RowScope.GlassExample(
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            color = GlassPalette.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 30.dp,
) {
    GlassPill(
        text = text,
        modifier = modifier,
        cornerRadius = cornerRadius,
        onClick = onClick
    )
}

@Composable
private fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    cornerRadius: Dp = if (compact) 24.dp else 30.dp,
    onClick: (() -> Unit)? = null
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 58.dp else 62.dp),
        cornerRadius = cornerRadius,
        onClick = onClick
    ) {
        Text(
            text = text,
            color = GlassPalette.Text,
            fontSize = if (compact) 19.sp else 21.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GlassIconButton() {
    GlassSurface(
        modifier = Modifier.size(78.dp),
        cornerRadius = 39.dp,
        shape = CircleShape,
        shadowStrength = 0.86f
    ) {
        Canvas(modifier = Modifier.size(38.dp)) {
            val stroke = Stroke(width = 6.dp.toPx())
            drawLine(
                color = Color.White.copy(alpha = 0.88f),
                start = Offset(size.width * 0.5f, size.height * 0.14f),
                end = Offset(size.width * 0.5f, size.height * 0.86f),
                strokeWidth = stroke.width
            )
            drawLine(
                color = Color.White.copy(alpha = 0.88f),
                start = Offset(size.width * 0.14f, size.height * 0.5f),
                end = Offset(size.width * 0.86f, size.height * 0.5f),
                strokeWidth = stroke.width
            )
        }
    }
}

@Composable
private fun GlassSelect(text: String) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        cornerRadius = 30.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = GlassPalette.Text,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium
            )
            Canvas(modifier = Modifier.size(18.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.26f)
                    lineTo(size.width * 0.92f, size.height * 0.26f)
                    lineTo(size.width * 0.50f, size.height * 0.82f)
                    close()
                }
                drawPath(path, GlassPalette.SecondaryText.copy(alpha = 0.72f))
                drawPath(path, GlassPalette.Hairline.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun GlassTextField(text: String) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        cornerRadius = 24.dp,
        shadowStrength = 0.78f
    ) {
        Text(
            text = text,
            color = GlassPalette.Placeholder,
            fontSize = 19.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun GlassTabs() {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        cornerRadius = 24.dp,
        shadowStrength = 0.82f
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color.White.copy(alpha = 0.34f))
                    .border(
                        width = 1.dp,
                        brush = SolidColor(Color.White.copy(alpha = 0.36f)),
                        shape = RoundedCornerShape(21.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Tab 1", color = GlassPalette.Text, fontSize = 18.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text("Tab 2", color = GlassPalette.Text, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun GlassSwitch() {
    GlassSurface(
        modifier = Modifier
            .width(146.dp)
            .height(58.dp),
        cornerRadius = 29.dp,
        shadowStrength = 0.76f
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = 47.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = GlassPalette.ShadowAmbient,
                        spotColor = GlassPalette.ShadowSpot
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.92f), Color(0xFFE8E5DE).copy(alpha = 0.88f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.66f), CircleShape)
            )
        }
    }
}

@Composable
private fun GlassSurface(
    modifier: Modifier,
    cornerRadius: Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(cornerRadius),
    shadowStrength: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shader = remember {
        Shader(glassButtonShaderSource).getRuntimeShader(
            uniforms = listOf(
                Uniform(Uniform.Type.SHADER, "image"),
                Uniform(Uniform.Type.VEC2, "resolution"),
                Uniform(Uniform.Type.FLOAT, "cornerRadius"),
                Uniform(Uniform.Type.VEC2, "lightPosition"),
                Uniform(Uniform.Type.VEC3, "lightColor"),
                Uniform(Uniform.Type.FLOAT, "shadowStrength"),
                Uniform(Uniform.Type.FLOAT, "time"),
                Uniform(Uniform.Type.FLOAT, "press"),
            ),
            customFunctions = emptySet(),
        )
    }

    val timeState = provideTimeAsState()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = 560f, dampingRatio = 0.78f),
        label = "glassPress"
    )

    val clickModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = (16 * shadowStrength).dp,
                shape = shape,
                clip = false,
                ambientColor = GlassPalette.ShadowAmbient.copy(alpha = 0.10f * shadowStrength),
                spotColor = GlassPalette.ShadowSpot.copy(alpha = 0.18f * shadowStrength)
            )
            .shadow(
                elevation = (5 * shadowStrength).dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Transparent,
                spotColor = Color(0xFF28343B).copy(alpha = 0.07f * shadowStrength)
            )
            .clip(shape)
            .onSizeChanged { size ->
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
            }
            .graphicsLayer {
                val lightX = size.width * 0.12f
                val lightY = -size.height * 0.52f
                shader.setFloatUniform("cornerRadius", cornerRadius.toPx())
                shader.setFloatUniform("lightPosition", lightX, lightY)
                shader.setFloatUniform(
                    "lightColor",
                    GlassPalette.LightTint.red,
                    GlassPalette.LightTint.green,
                    GlassPalette.LightTint.blue
                )
                shader.setFloatUniform("shadowStrength", shadowStrength)
                shader.setFloatUniform("time", timeState.value)
                shader.setFloatUniform("press", press)
                scaleX = 1f - press * 0.012f
                scaleY = 1f - press * 0.012f
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            }
            .then(clickModifier)
            .background(
                Brush.verticalGradient(
                    listOf(GlassPalette.GlassTop, GlassPalette.GlassBottom)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(GlassPalette.Hairline, GlassPalette.CoolLine)
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
