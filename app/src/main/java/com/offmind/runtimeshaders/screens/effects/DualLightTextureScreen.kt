package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import org.intellij.lang.annotations.Language

private data class LightDemoPage(
    val style: LightDemoPageStyle,
    val title: String,
    val description: String,
    val panelTitle: String,
    val panelDescription: String,
    val primaryAction: String,
    val secondaryAction: String,
    val firstToggleTitle: String,
    val secondToggleTitle: String
)

private enum class LightDemoPageStyle {
    Standard,
    MixedControls,
    ImagePreview,
    CompactStatus,
    Reading
}

private val lightDemoPages = listOf(
    LightDemoPage(
        style = LightDemoPageStyle.Standard,
        title = "Shared light state",
        description = "A single lighting state is passed into every component. Pink light falls from the top and blue light rises from the bottom without replacing the original UI.",
        panelTitle = "Screen space",
        panelDescription = "Buttons, toggles, and text surfaces reuse the same shader and receive their own physical position.",
        primaryAction = "Primary",
        secondaryAction = "Secondary",
        firstToggleTitle = "Ambient mix",
        secondToggleTitle = "Surface detail"
    ),
    LightDemoPage(
        style = LightDemoPageStyle.MixedControls,
        title = "Interactive surfaces",
        description = "This page mixes one toggle with small directional controls and status chips, so the shader touches several compact shapes.",
        panelTitle = "Local components",
        panelDescription = "Each control reports a different screen-space origin into the same lighting model.",
        primaryAction = "Accept",
        secondaryAction = "Dismiss",
        firstToggleTitle = "Glow edges",
        secondToggleTitle = "Panel lift"
    ),
    LightDemoPage(
        style = LightDemoPageStyle.ImagePreview,
        title = "Reflection pass",
        description = "Image surfaces keep their original content while the light shader adds color from the fixed lamps.",
        panelTitle = "Inverse falloff",
        panelDescription = "Light intensity decays with an inverse cubic curve and color is normalized through HSV.",
        primaryAction = "Enable",
        secondaryAction = "Preview",
        firstToggleTitle = "HSV tint",
        secondToggleTitle = "Soft rim"
    ),
    LightDemoPage(
        style = LightDemoPageStyle.CompactStatus,
        title = "Status cluster",
        description = "A denser layout makes the top and bottom glows easier to compare across neighboring elements.",
        panelTitle = "Grouped controls",
        panelDescription = "Small tiles, pills, and action surfaces all use the same per-element shader.",
        primaryAction = "Sync",
        secondaryAction = "Queue",
        firstToggleTitle = "Live mode",
        secondToggleTitle = "Depth cue"
    ),
    LightDemoPage(
        style = LightDemoPageStyle.Reading,
        title = "Reading surface",
        description = "Text-heavy content stays readable while still picking up the scene lighting.",
        panelTitle = "Paragraph block",
        panelDescription = "This variation checks how subtle light behaves on longer copy, labels, and quiet controls.",
        primaryAction = "Save",
        secondaryAction = "Share",
        firstToggleTitle = "Focus mode",
        secondToggleTitle = "Annotations"
    )
)

@Immutable
private data class LightSource(
    val position: Offset,
    val color: Color,
    val radius: Float,
    val intensity: Float
)

@Immutable
private data class DualLightingState(
    val topLight: LightSource,
    val bottomLight: LightSource,
    val screenSize: Size
) {
    companion object {
        val Empty = DualLightingState(
            topLight = LightSource(
                position = Offset.Zero,
                color = Color(0xFFFF2DBB),
                radius = 1f,
                intensity = 0f
            ),
            bottomLight = LightSource(
                position = Offset.Zero,
                color = Color(0xFF5FEFFF),
                radius = 1f,
                intensity = 0f
            ),
            screenSize = Size.Zero
        )
    }
}

@Composable
fun DualLightTextureScreen() {
    var screenSize by remember { mutableStateOf(Size.Zero) }
    val lightingState = remember(screenSize) {
        val width = screenSize.width.coerceAtLeast(1f)
        val height = screenSize.height.coerceAtLeast(1f)

        DualLightingState(
            topLight = LightSource(
                position = Offset(width * 0.5f, -height * 0.07f),
                color = Color(0xFFFF2DBB),
                radius = height * 0.36f,
                intensity = 1.28f
            ),
            bottomLight = LightSource(
                position = Offset(width * 0.5f, height * 1.07f),
                color = Color(0xFF5FEFFF),
                radius = height * 0.36f,
                intensity = 1.18f
            ),
            screenSize = screenSize
        )
    }

    val backgroundShader = remember {
        Shader(dualLightBackgroundShader).getRuntimeShader(
            uniforms = lightingUniforms()
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                screenSize = Size(
                    width = it.width.toFloat(),
                    height = it.height.toFloat()
                )
            }
            .background(Color(0xFF11161F)),
        contentAlignment = Alignment.Center
    ) {
        ShadedBox(
            modifier = Modifier.fillMaxSize(),
            shader = backgroundShader,
            shaderUniforms = lightingUniformValues(lightingState),
            includeTime = true,
        ) {
            Image(
                painter = painterResource(R.drawable.gray_wall_texture),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        val pagerState = rememberPagerState(pageCount = { lightDemoPages.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 18.dp,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            LightDemoContent(
                lightingState = lightingState,
                page = lightDemoPages[page],
                modifier = Modifier
                    .padding(vertical = 96.dp)
                    .fillMaxWidth()
            )
        }
    }
}

val SURFACE_STRENGTH = 0.7f

@Composable
private fun LightDemoContent(
    lightingState: DualLightingState,
    page: LightDemoPage,
    modifier: Modifier = Modifier
) {
    var ambientEnabled by remember(page.title) { mutableStateOf(true) }
    var detailEnabled by remember(page.title) { mutableStateOf(false) }
    var selectedAction by remember(page.title) { mutableStateOf(page.primaryAction) }

    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF171C26).copy(alpha = 0.82f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = page.title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = page.description,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            }

            when (page.style) {
                LightDemoPageStyle.Standard -> StandardPageBody(
                    lightingState = lightingState,
                    page = page,
                    selectedAction = selectedAction,
                    onSelectedActionChange = { selectedAction = it },
                    ambientEnabled = ambientEnabled,
                    onAmbientChange = { ambientEnabled = it },
                    detailEnabled = detailEnabled,
                    onDetailChange = { detailEnabled = it }
                )

                LightDemoPageStyle.MixedControls -> MixedControlsPageBody(
                    lightingState = lightingState,
                    page = page,
                    selectedAction = selectedAction,
                    onSelectedActionChange = { selectedAction = it },
                    ambientEnabled = ambientEnabled,
                    onAmbientChange = { ambientEnabled = it }
                )

                LightDemoPageStyle.ImagePreview -> ImagePreviewPageBody(
                    lightingState = lightingState,
                    page = page,
                    selectedAction = selectedAction,
                    onSelectedActionChange = { selectedAction = it }
                )

                LightDemoPageStyle.CompactStatus -> CompactStatusPageBody(
                    lightingState = lightingState,
                    page = page,
                    selectedAction = selectedAction,
                    onSelectedActionChange = { selectedAction = it },
                    detailEnabled = detailEnabled,
                    onDetailChange = { detailEnabled = it }
                )

                LightDemoPageStyle.Reading -> ReadingPageBody(
                    lightingState = lightingState,
                    page = page,
                    selectedAction = selectedAction,
                    onSelectedActionChange = { selectedAction = it },
                    ambientEnabled = ambientEnabled,
                    onAmbientChange = { ambientEnabled = it }
                )
            }
        }
    }
}

@Composable
private fun StandardPageBody(
    lightingState: DualLightingState,
    page: LightDemoPage,
    selectedAction: String,
    onSelectedActionChange: (String) -> Unit,
    ambientEnabled: Boolean,
    onAmbientChange: (Boolean) -> Unit,
    detailEnabled: Boolean,
    onDetailChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LightTextPanel(
            lightingState = lightingState,
            title = page.panelTitle,
            description = page.panelDescription
        )
        LightActionRow(
            lightingState = lightingState,
            page = page,
            selectedAction = selectedAction,
            onSelectedActionChange = onSelectedActionChange
        )
        LightToggleRow(
            lightingState = lightingState,
            title = page.firstToggleTitle,
            description = "Soft additive contribution",
            checked = ambientEnabled,
            onCheckedChange = onAmbientChange
        )
        LightToggleRow(
            lightingState = lightingState,
            title = page.secondToggleTitle,
            description = "Extra response on brighter controls",
            checked = detailEnabled,
            onCheckedChange = onDetailChange
        )
    }
}

@Composable
private fun MixedControlsPageBody(
    lightingState: DualLightingState,
    page: LightDemoPage,
    selectedAction: String,
    onSelectedActionChange: (String) -> Unit,
    ambientEnabled: Boolean,
    onAmbientChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LightToggleRow(
            lightingState = lightingState,
            title = page.firstToggleTitle,
            description = "One toggle with nearby non-toggle surfaces",
            checked = ambientEnabled,
            onCheckedChange = onAmbientChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LightMetricTile(
                lightingState = lightingState,
                label = "Top lamp",
                value = "128%",
                modifier = Modifier.weight(1f)
            )
            LightMetricTile(
                lightingState = lightingState,
                label = "Bottom lamp",
                value = "118%",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LightChip(lightingState, "Screen", Modifier.weight(1f))
            LightChip(lightingState, "HSV", Modifier.weight(1f))
            LightChip(lightingState, "Cubic", Modifier.weight(1f))
        }
        LightActionRow(
            lightingState = lightingState,
            page = page,
            selectedAction = selectedAction,
            onSelectedActionChange = onSelectedActionChange
        )
    }
}

@Composable
private fun ImagePreviewPageBody(
    lightingState: DualLightingState,
    page: LightDemoPage,
    selectedAction: String,
    onSelectedActionChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LightImagePanel(
            lightingState = lightingState,
            title = page.panelTitle,
            description = page.panelDescription
        )
        LightActionRow(
            lightingState = lightingState,
            page = page,
            selectedAction = selectedAction,
            onSelectedActionChange = onSelectedActionChange
        )
    }
}

@Composable
private fun CompactStatusPageBody(
    lightingState: DualLightingState,
    page: LightDemoPage,
    selectedAction: String,
    onSelectedActionChange: (String) -> Unit,
    detailEnabled: Boolean,
    onDetailChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LightMetricTile(lightingState, "Hue", "0.88", Modifier.weight(1f))
            LightMetricTile(lightingState, "Falloff", "1/d3", Modifier.weight(1f))
        }
        LightTextPanel(
            lightingState = lightingState,
            title = page.panelTitle,
            description = page.panelDescription
        )
        LightToggleRow(
            lightingState = lightingState,
            title = page.secondToggleTitle,
            description = "A single switch inside a compact status page",
            checked = detailEnabled,
            onCheckedChange = onDetailChange
        )
        LightActionRow(
            lightingState = lightingState,
            page = page,
            selectedAction = selectedAction,
            onSelectedActionChange = onSelectedActionChange
        )
    }
}

@Composable
private fun ReadingPageBody(
    lightingState: DualLightingState,
    page: LightDemoPage,
    selectedAction: String,
    onSelectedActionChange: (String) -> Unit,
    ambientEnabled: Boolean,
    onAmbientChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LightTextPanel(
            lightingState = lightingState,
            title = page.panelTitle,
            description = page.panelDescription
        )
        LightToggleRow(
            lightingState = lightingState,
            title = page.firstToggleTitle,
            description = "Quiet content surface with lighting",
            checked = ambientEnabled,
            onCheckedChange = onAmbientChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LightChip(lightingState, "Readable", Modifier.weight(1f))
            LightChip(lightingState, "Subtle", Modifier.weight(1f))
        }
        LightActionRow(
            lightingState = lightingState,
            page = page,
            selectedAction = selectedAction,
            onSelectedActionChange = onSelectedActionChange
        )
    }
}

@Composable
private fun LightActionRow(
    lightingState: DualLightingState,
    page: LightDemoPage,
    selectedAction: String,
    onSelectedActionChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LightActionButton(
            lightingState = lightingState,
            text = page.primaryAction,
            selected = selectedAction == page.primaryAction,
            onClick = { onSelectedActionChange(page.primaryAction) },
            modifier = Modifier.weight(1f)
        )
        LightActionButton(
            lightingState = lightingState,
            text = page.secondaryAction,
            selected = selectedAction == page.secondaryAction,
            onClick = { onSelectedActionChange(page.secondaryAction) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LightTextPanel(
    lightingState: DualLightingState,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF222938).copy(alpha = 0.88f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun LightImagePanel(
    lightingState: DualLightingState,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF222938).copy(alpha = 0.88f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.generic_nature_2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun LightMetricTile(
    lightingState: DualLightingState,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier.height(86.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF202633).copy(alpha = 0.88f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun LightChip(
    lightingState: DualLightingState,
    text: String,
    modifier: Modifier = Modifier
) {
    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier.height(42.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFF252C3B).copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LightActionButton(
    lightingState: DualLightingState,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color(0xFFE9EEF8) else Color(0xFF222938)
    val foreground = if (selected) Color(0xFF121722) else Color.White.copy(alpha = 0.88f)

    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier.height(58.dp)
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = background,
                contentColor = foreground
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = if (selected) 0.32f else 0.14f)
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LightToggleRow(
    lightingState: DualLightingState,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ReactiveLightBox(
        lightingState = lightingState,
        surfaceStrength = SURFACE_STRENGTH,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF202633).copy(alpha = 0.88f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF5FEFFF).copy(alpha = 0.78f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.72f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.14f)
                )
            )
        }
    }
}

@Composable
private fun ReactiveLightBox(
    lightingState: DualLightingState,
    surfaceStrength: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shader = remember {
        Shader(elementLightShader).getRuntimeShader(
            uniforms = lightingUniforms()
                .addUniform(Uniform.Type.VEC2 to "elementOrigin")
                .addUniform(Uniform.Type.FLOAT to "surfaceStrength")
        )
    }

    ShadedBox(
        modifier = modifier.onGloballyPositioned {
            origin = it.positionInRoot()
        },
        shader = shader,
        shaderUniforms = lightingUniformValues(lightingState) + mapOf(
            "elementOrigin" to ShaderTypedValue.Vec2Type(origin.x, origin.y),
            "surfaceStrength" to ShaderTypedValue.FloatType(surfaceStrength)
        ),
        includeTime = true,
        content = content
    )
}

@Composable
private fun LightBar(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.74f)
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.65f),
                shape = RoundedCornerShape(4.dp)
            )
    )
}

private fun lightingUniforms(): List<Uniform> {
    return basicUniformList
        .addUniform(Uniform.Type.VEC2 to "screenResolution")
        .addUniform(Uniform.Type.VEC2 to "topLightPosition")
        .addUniform(Uniform.Type.VEC3 to "topLightColor")
        .addUniform(Uniform.Type.FLOAT to "topLightRadius")
        .addUniform(Uniform.Type.FLOAT to "topLightIntensity")
        .addUniform(Uniform.Type.VEC2 to "bottomLightPosition")
        .addUniform(Uniform.Type.VEC3 to "bottomLightColor")
        .addUniform(Uniform.Type.FLOAT to "bottomLightRadius")
        .addUniform(Uniform.Type.FLOAT to "bottomLightIntensity")
}

private fun lightingUniformValues(
    state: DualLightingState
): Map<String, ShaderTypedValue> {
    return mapOf(
        "screenResolution" to ShaderTypedValue.Vec2Type(
            state.screenSize.width,
            state.screenSize.height
        ),
        "topLightPosition" to ShaderTypedValue.Vec2Type(
            state.topLight.position.x,
            state.topLight.position.y
        ),
        "topLightColor" to ShaderTypedValue.Vec3Type(
            state.topLight.color.red,
            state.topLight.color.green,
            state.topLight.color.blue
        ),
        "topLightRadius" to ShaderTypedValue.FloatType(state.topLight.radius),
        "topLightIntensity" to ShaderTypedValue.FloatType(state.topLight.intensity),
        "bottomLightPosition" to ShaderTypedValue.Vec2Type(
            state.bottomLight.position.x,
            state.bottomLight.position.y
        ),
        "bottomLightColor" to ShaderTypedValue.Vec3Type(
            state.bottomLight.color.red,
            state.bottomLight.color.green,
            state.bottomLight.color.blue
        ),
        "bottomLightRadius" to ShaderTypedValue.FloatType(state.bottomLight.radius),
        "bottomLightIntensity" to ShaderTypedValue.FloatType(state.bottomLight.intensity)
    )
}

@Language("AGSL")
private val dualLightBackgroundShader = """
    float temporalNoise(float value, float seed) {
        float cell = floor(value);
        float fraction = fract(value);
        fraction = fraction * fraction * (3.0 - 2.0 * fraction);
        float a = Hash21(vec2(cell, seed));
        float b = Hash21(vec2(cell + 1.0, seed));
        return mix(a, b, fraction);
    }

    float lightFlicker(float value, float seed) {
        float slow = temporalNoise(value * 1.6, seed);
        float mid = temporalNoise(value * 4.7, seed + 17.0);
        float fast = temporalNoise(value * 11.3, seed + 41.0);
        float spark = smoothstep(0.82, 1.0, temporalNoise(value * 7.9, seed + 83.0));
        return 0.88 + slow * 0.10 + mid * 0.06 + fast * 0.035 + spark * 0.08;
    }

    vec3 lightAt(vec2 position, vec2 lightPosition, vec3 lightColor, float radius, float intensity, float flickerSeed) {
        float distanceToLight = length(position - lightPosition);
        float normalizedDistance = distanceToLight / radius;
        float inverseCube = 1.0 / (1.0 + 5.5 * normalizedDistance * normalizedDistance * normalizedDistance);
        float softLimit = 1.0 - smoothstep(1.35, 2.25, normalizedDistance);
        float falloff = inverseCube * softLimit;
        float flicker = lightFlicker(time, flickerSeed);

        vec3 hsv = RGBtoHSV(lightColor);
        hsv.y = clamp(hsv.y * 0.92, 0.0, 1.0);
        hsv.z = 1.0;
        return HSVtoRGB(hsv) * falloff * intensity * flicker;
    }

    vec4 main(float2 fragCoord) {
        vec2 uv = fragCoord / resolution;
        vec4 sampled = image.eval(fragCoord);

        vec3 base = sampled.rgb * vec3(0.34, 0.38, 0.44);
        vec3 light = lightAt(fragCoord, topLightPosition, topLightColor, topLightRadius, topLightIntensity, 11.3);
        light += lightAt(fragCoord, bottomLightPosition, bottomLightColor, bottomLightRadius, bottomLightIntensity, 29.7);

        float vignette = smoothstep(0.92, 0.25, length(uv - vec2(0.5)));
        vec3 color = base * (0.48 + vignette * 0.24);
        color += light * 0.36;
        color = mix(color, color * vec3(0.72, 0.76, 0.84), smoothstep(0.22, 0.75, uv.y) * 0.32);

        return vec4(color, sampled.a);
    }
""".trimIndent()

@Language("AGSL")
private val elementLightShader = """
    float temporalNoise(float value, float seed) {
        float cell = floor(value);
        float fraction = fract(value);
        fraction = fraction * fraction * (3.0 - 2.0 * fraction);
        float a = Hash21(vec2(cell, seed));
        float b = Hash21(vec2(cell + 1.0, seed));
        return mix(a, b, fraction);
    }

    float lightFlicker(float value, float seed) {
        float slow = temporalNoise(value * 1.6, seed);
        float mid = temporalNoise(value * 4.7, seed + 17.0);
        float fast = temporalNoise(value * 11.3, seed + 41.0);
        float spark = smoothstep(0.82, 1.0, temporalNoise(value * 7.9, seed + 83.0));
        return 0.88 + slow * 0.10 + mid * 0.06 + fast * 0.035 + spark * 0.08;
    }

    vec3 lightAt(vec2 position, vec2 lightPosition, vec3 lightColor, float radius, float intensity, float flickerSeed) {
        float distanceToLight = length(position - lightPosition);
        float normalizedDistance = distanceToLight / radius;
        float inverseCube = 1.0 / (1.0 + 5.0 * normalizedDistance * normalizedDistance * normalizedDistance);
        float softLimit = 1.0 - smoothstep(1.4, 2.3, normalizedDistance);
        float falloff = inverseCube * softLimit;
        float flicker = lightFlicker(time, flickerSeed);

        vec3 hsv = RGBtoHSV(lightColor);
        hsv.y = clamp(hsv.y * 0.9, 0.0, 1.0);
        hsv.z = 1.0;
        return HSVtoRGB(hsv) * falloff * intensity * flicker;
    }

    vec4 main(float2 fragCoord) {
        vec2 localUv = fragCoord / resolution;
        vec2 screenPosition = elementOrigin + fragCoord;
        vec4 sampled = image.eval(fragCoord);

        vec3 topLight = lightAt(screenPosition, topLightPosition, topLightColor, topLightRadius, topLightIntensity, 11.3);
        vec3 bottomLight = lightAt(screenPosition, bottomLightPosition, bottomLightColor, bottomLightRadius, bottomLightIntensity, 29.7);
        vec3 addedLight = (topLight + bottomLight) * surfaceStrength;

        float topEdge = 1.0 - smoothstep(0.0, 0.72, localUv.y);
        float bottomEdge = smoothstep(0.28, 1.0, localUv.y);
        float sideRim = smoothstep(0.46, 0.0, abs(localUv.x - 0.5));
        float edgeResponse = 0.64 + topEdge * 0.72 + bottomEdge * 0.72 + sideRim * 0.18;
        vec3 color = sampled.rgb + addedLight * edgeResponse * sampled.a;
        color += topLight * topEdge * surfaceStrength * 0.18 * sampled.a;
        color += bottomLight * bottomEdge * surfaceStrength * 0.18 * sampled.a;

        return vec4(clamp(color, 0.0, 1.0), sampled.a);
    }
""".trimIndent()
