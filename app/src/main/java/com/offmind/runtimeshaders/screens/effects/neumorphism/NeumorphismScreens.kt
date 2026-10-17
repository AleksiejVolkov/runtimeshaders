package com.offmind.runtimeshaders.screens.effects.neumorphism

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    StatusBarIcons(darkIcons = !dark)
    // the theme is just a different set of colors: animate between the two palettes
    val theme by animateFloatAsState(
        targetValue = if (dark) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "theme"
    )
    val palette = remember { derivedStateOf { lerp(LightPalette, DarkPalette, theme) } }
    // the light follows the finger and springs back when it's released
    var target by remember { mutableStateOf(DefaultLight) }
    val light = animateOffsetAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "light"
    )
    CompositionLocalProvider(
        LocalLightDirection provides light,
        LocalNeuPalette provides palette,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind { drawRect(palette.value.base) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Initial pass: we only watch the finger, buttons still get their clicks
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val finger = event.changes.first().position
                            val fromCenter = finger - size.center.toOffset()
                            if (fromCenter.getDistance() > 1f) {
                                target = fromCenter / fromCenter.getDistance()
                            }
                        } while (event.changes.any { it.pressed })
                        target = DefaultLight
                    }
                },
            content = content
        )
    }
}

@Composable
fun NeumorphismBasicsScreen() {
    NeuSurface {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(56.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                NeuSample("FLAT") { NeuShape(cornerRadius = 22.dp) }
                NeuSample("CONCAVE") { NeuShape(cornerRadius = 55.dp, curve = -0.075f) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                NeuSample("CONCAVE") { NeuShape(cornerRadius = 22.dp, curve = -0.075f) }
                NeuSample("PRESSED") { NeuShape(cornerRadius = 22.dp, pressedByDefault = true) }
            }
        }
    }
}

@Composable
fun NeumorphismThermostatScreen() {
    var target by remember { mutableIntStateOf(22) }
    var nightMode by remember { mutableStateOf(false) }
    var ecoMode by remember { mutableStateOf(true) }

    NeuSurface(dark = nightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text("Living room", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = neuColors().text)
                Text("Heating · 4 devices", fontSize = 14.sp, color = neuColors().hint)
            }
            Spacer(Modifier.height(56.dp))

            // the dial: one big concave dish
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .neumorphic(cornerRadius = 110.dp, curve = -0.075f, distance = 14.dp, blur = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$target°", fontSize = 64.sp, fontWeight = FontWeight.Light, color = neuColors().text)
                    Text("target", fontSize = 13.sp, color = neuColors().hint)
                }
            }
            Spacer(Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                NeuButton(onClick = { target-- }) { pressed ->
                    Text("\u2212", fontSize = 32.sp, color = iconColor(pressed))
                }
                NeuButton(onClick = { target++ }) { pressed ->
                    Text("+", fontSize = 32.sp, color = iconColor(pressed))
                }
            }
            Spacer(Modifier.height(56.dp))

            // flat cards with toggles
            NeuSettingCard("Night mode", "Dark theme for late hours", nightMode) { nightMode = it }
            Spacer(Modifier.height(24.dp))
            NeuSettingCard("Eco mode", "Keep 19° while nobody is home", ecoMode) { ecoMode = it }
        }
    }
}

// the icon gets more contrast while the button is pressed in
@Composable
private fun iconColor(pressed: Boolean) = if (pressed) neuColors().text else neuColors().hint

@Composable
private fun NeuSettingCard(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .neumorphic(cornerRadius = 24.dp, distance = 8.dp, blur = 18.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = neuColors().text)
            Text(subtitle, fontSize = 13.sp, color = neuColors().hint)
        }
        NeuToggle(checked = checked, onCheckedChange = onChange)
    }
}

// a shape you can press: the press state goes straight into the shader
@Composable
private fun NeuShape(cornerRadius: Dp, curve: Float = 0f, pressedByDefault: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (isPressed != pressedByDefault) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "press"
    )
    Box(
        Modifier
            .size(110.dp)
            .neumorphic(cornerRadius, curve = curve, press = press)
            .clickable(interaction, indication = null) {}
    )
}

@Composable
private fun NeuSample(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
        Spacer(Modifier.height(28.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.SemiBold,
            color = neuColors().hint
        )
    }
}

// a light material needs dark status bar icons, and the other way round
@Composable
private fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, darkIcons) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val before = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = darkIcons
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
