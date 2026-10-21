package com.offmind.runtimeshaders.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.screens.effects.glass.GlassBackdrop
import com.offmind.runtimeshaders.screens.effects.glass.DayMood
import com.offmind.runtimeshaders.screens.effects.glass.NightMood
import com.offmind.runtimeshaders.screens.effects.glass.glass
import com.offmind.runtimeshaders.screens.effects.neumorphism.LightPalette
import com.offmind.runtimeshaders.screens.effects.neumorphism.DarkPalette
import com.offmind.runtimeshaders.screens.effects.neumorphism.LocalNeuPalette
import com.offmind.runtimeshaders.screens.effects.neumorphism.neumorphic
import com.offmind.runtimeshaders.ui.theme.AppAppearance
import com.offmind.runtimeshaders.ui.theme.LocalAppAppearance
import com.offmind.runtimeshaders.ui.theme.LocalAppDarkTheme

/** Theme only the app's chrome; demo screens keep the materials they are demonstrating. */
@Composable
internal fun AppAppearanceTheme(
    appearance: AppAppearance = LocalAppAppearance.current,
    content: @Composable () -> Unit,
) {
    val darkTheme = appearance.usesDarkTheme(isSystemInDarkTheme())
    val neuPalette = if (darkTheme) DarkPalette else LightPalette
    val glassMood = if (darkTheme) NightMood else DayMood
    val colors = when (appearance) {
        AppAppearance.Neumorphism -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFA9D8EA),
            background = neuPalette.base,
            surface = neuPalette.base,
            onBackground = neuPalette.text,
            onSurface = neuPalette.text,
            onSurfaceVariant = Color(0xFFB2BCCB),
        ) else lightColorScheme(
            primary = Color(0xFF365F70),
            background = neuPalette.base,
            surface = neuPalette.base,
            onBackground = neuPalette.text,
            onSurface = neuPalette.text,
            onSurfaceVariant = Color(0xFF626D80),
        )
        AppAppearance.Glass -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFBEEBF7),
            background = glassMood.bottom,
            surface = glassMood.top,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCFD8E2),
        ) else lightColorScheme(
            primary = Color(0xFF1B5871),
            background = glassMood.bottom,
            surface = glassMood.top,
            onBackground = Color(0xFF20364B),
            onSurface = Color(0xFF20364B),
            onSurfaceVariant = Color(0xFF435C70),
        )
        AppAppearance.Classic -> darkColorScheme(
            primary = Color(0xFFB3DDF5),
            background = Color(0xFF0A0D13),
            surface = Color(0xFF111923),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFBFC3CA),
        )
    }
    CompositionLocalProvider(
        LocalAppAppearance provides appearance,
        LocalAppDarkTheme provides darkTheme,
        LocalNeuPalette provides rememberUpdatedState(neuPalette),
    ) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@Composable
internal fun AppScreen(
    showBranding: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    AppAppearanceTheme {
        Box(Modifier.fillMaxSize()) {
            AppBackground(Modifier.matchParentSize())
            if (showBranding && LocalAppAppearance.current == AppAppearance.Classic) {
                Image(
                    painter = painterResource(R.drawable.logo_label),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
                )
            }
            // Keep the content in the same composition position when the appearance changes.
            content()
        }
    }
}

@Composable
internal fun AppBackground(modifier: Modifier = Modifier) {
    when (LocalAppAppearance.current) {
        AppAppearance.Classic -> Box(modifier.background(MaterialTheme.colorScheme.background)) {
            Image(
                painter = painterResource(R.drawable.main_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
        }
        AppAppearance.Neumorphism -> Box(modifier.background(LocalNeuPalette.current.value.base))
        AppAppearance.Glass -> GlassBackdrop(
            mood = if (LocalAppDarkTheme.current) NightMood else DayMood,
            modifier = modifier,
        ) {}
    }
}

@Composable
internal fun AppSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    selected: Boolean? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(140), label = "surfacePress")
    val shape = RoundedCornerShape(cornerRadius)
    val darkTheme = LocalAppDarkTheme.current
    val surface = when (LocalAppAppearance.current) {
        AppAppearance.Classic -> Modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.6f - press * 0.15f))
            .border(1.dp, Color.White.copy(alpha = if (selected == true) 0.5f else 0.16f), shape)
        AppAppearance.Neumorphism -> Modifier.neumorphic(
            cornerRadius = cornerRadius,
            press = if (selected == true) 1f else press,
            distance = 5.dp,
            blur = 10.dp,
        )
        AppAppearance.Glass -> Modifier.glass(
            cornerRadius = cornerRadius,
            tint = when {
                selected != true -> Color.White
                darkTheme -> Color.Black
                else -> MaterialTheme.colorScheme.primary
            },
            tintAlpha = if (selected == true) {
                if (darkTheme) 0.2f else 0.08f
            } else 0.10f,
            lift = if (selected == true) 0.dp else 5.dp * (1f - press),
            depth = if (selected == true) -1f else 1f - press * 0.65f,
        )
    }
    val input = if (selected == null) {
        Modifier.clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
    } else {
        Modifier.selectable(selected, interaction, indication = null, role = Role.RadioButton, onClick = onClick)
    }
    Box(modifier.then(surface).then(input), content = content)
}

@Composable
internal fun AppIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSurface(onClick, modifier.size(48.dp), cornerRadius = 24.dp) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center).size(24.dp),
        )
    }
}

@Composable
internal fun AppMenuRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    detail: String? = null,
    selected: Boolean? = null,
) {
    AppSurface(onClick = onClick, modifier = modifier.fillMaxWidth(), selected = selected) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (description.isNotBlank()) {
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                if (detail != null) {
                    Text(detail, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
            if (selected != null) {
                RadioButton(selected = selected, onClick = null)
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Called for the active chrome destination only, so background back-stack entries cannot fight it. */
@Composable
internal fun AppSystemBars(darkIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, darkIcons) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val oldStatus = controller.isAppearanceLightStatusBars
        val oldNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
        onDispose {
            controller.isAppearanceLightStatusBars = oldStatus
            controller.isAppearanceLightNavigationBars = oldNavigation
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
