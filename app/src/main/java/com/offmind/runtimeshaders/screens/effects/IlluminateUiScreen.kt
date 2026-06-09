package com.offmind.runtimeshaders.screens.effects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.screens.effects.lighting.LightingScope
import com.offmind.runtimeshaders.screens.effects.lighting.LightingScopeReceiver

// ── Design tokens ─────────────────────────────────────────────────────────────

private object Palette {
    val Background = Color(0xFF0D1520)
    val Card = Color(0xFF1A2235)
    val CardActive = Color(0xFF1E1830)
    val Border = Color.White.copy(alpha = 0.10f)
    val AccentOrange = Color(0xFFFF5722)
    val AccentPurple = Color(0xFF8B6FD4)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.55f)
}

/** Tuning for the two demonstrable light sources on this screen. */
private object Lighting {
    const val ProjectIntensity = 0.5f
    const val ProjectBloomIntensity = 0.3f

    const val TaskIntensity = 0.25f
    const val TaskBloomIntensity = 0.1f
}

private const val ToggleDurationMillis = 600

/** Shared spec so every on/off transition on this screen animates identically. */
private fun <T> toggleSpec(): TweenSpec<T> =
    tween(durationMillis = ToggleDurationMillis, easing = FastOutSlowInEasing)

// ── Data ──────────────────────────────────────────────────────────────────────

private data class ActivityItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
)

/** A short list repeated twice, just to give the fade-edged list something to scroll. */
private val activityItems: List<ActivityItem> = List(2) {
    listOf(
        "UI lighting model" to "Updated 2m ago",
        "Compose shaders" to "Updated 1h ago",
        "Surface response" to "Updated 3h ago",
        "Edge glow pass" to "Updated 5h ago",
        "Shader uniforms" to "Updated 1d ago",
    ).map { (title, subtitle) ->
        ActivityItem(title, subtitle, Icons.AutoMirrored.Filled.List, Palette.AccentPurple)
    }
}.flatten()

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun IlluminateUiScreen() {
    var projectEnabled by remember { mutableStateOf(true) }
    var tasksEnabled by remember { mutableStateOf(false) }

    val taskIntensity by animateFloatAsState(
        targetValue = if (tasksEnabled) Lighting.TaskIntensity else 0f,
        animationSpec = toggleSpec(),
        label = "taskLight",
    )
    val taskBloom by animateFloatAsState(
        targetValue = if (tasksEnabled) Lighting.TaskBloomIntensity else 0f,
        animationSpec = toggleSpec(),
        label = "taskBloom",
    )

    LightingScope(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header()

            ProjectCard(
                enabled = projectEnabled,
                onToggle = { projectEnabled = it },
            )

            MessageCard(
                title = "Tasks",
                subtitle = "12 pending",
                icon = Icons.Default.CheckCircle,
                iconTint = Palette.AccentPurple.copy(alpha = 0.7f),
                checked = tasksEnabled,
                onCheckedChange = { tasksEnabled = it },
                switchModifier = Modifier.lightSource(
                    color = Palette.AccentPurple,
                    intensity = taskIntensity,
                    bloomIntensity = taskBloom,
                ),
            )

            Text(
                text = "Recent Activity",
                color = Palette.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalEdgeFade(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                items(activityItems) { item -> ActivityRow(item = item) }
            }
        }
    }
}

// ── Components ─────────────────────────────────────────────────────────────────

@Composable
private fun LightingScopeReceiver.Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Good morning,", color = Palette.TextSecondary, fontSize = 16.sp)
            Text(
                text = "Compose Dev",
                color = Palette.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .receivesLight(strength = 0.1f)
                .background(Palette.Card)
                .border(1.dp, Palette.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Palette.TextSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun LightingScopeReceiver.ProjectCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lightIntensity by animateFloatAsState(
        targetValue = if (enabled) Lighting.ProjectIntensity else 0f,
        animationSpec = toggleSpec(),
        label = "projectLight",
    )
    val bloomIntensity by animateFloatAsState(
        targetValue = if (enabled) Lighting.ProjectBloomIntensity else 0f,
        animationSpec = toggleSpec(),
        label = "projectBloom",
    )
    val cardBg by animateColorAsState(
        targetValue = if (enabled) Palette.CardActive else Palette.Card,
        animationSpec = toggleSpec(),
        label = "projectCardBg",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (enabled) 0.85f else 0.3f,
        animationSpec = toggleSpec(),
        label = "projectIconAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .receivesLight(strength = 1f)
            .background(cardBg)
            .border(1.dp, Palette.Border, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Active Project", color = Palette.TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Illuminate UI", color = Palette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Global lighting with shaders", color = Palette.TextSecondary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.AccentOrange.copy(alpha = iconAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.sun_ic),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .lightSource(
                            color = Palette.AccentOrange,
                            intensity = lightIntensity,
                            bloomIntensity = bloomIntensity,
                        ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = accentSwitchColors(Palette.AccentOrange),
            )
        }
    }
}

@Composable
private fun LightingScopeReceiver.MessageCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    switchModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .receivesLight(strength = 0.7f)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Palette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Palette.TextSecondary, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = switchModifier,
            colors = accentSwitchColors(Palette.AccentPurple),
        )
    }
}

@Composable
private fun LightingScopeReceiver.ActivityRow(
    item: ActivityItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .receivesLight(strength = 1f)
            .shadow(elevation = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .receivesLight(strength = 6.6f)
                .background(item.iconTint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = item.title, color = Palette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = item.subtitle, color = Palette.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun accentSwitchColors(accent: Color): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = accent.copy(alpha = 0.75f),
    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
    uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
)
