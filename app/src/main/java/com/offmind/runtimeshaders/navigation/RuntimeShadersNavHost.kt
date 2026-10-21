package com.offmind.runtimeshaders.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.rememberNavBackStack
import com.offmind.runtimeshaders.data.AppSettingsRepository
import com.offmind.runtimeshaders.ui.components.AppSystemBars
import com.offmind.runtimeshaders.ui.theme.AppAppearance
import com.offmind.runtimeshaders.ui.theme.LocalAppAppearance
import kotlinx.coroutines.launch

@Composable
internal fun RuntimeShadersNavHost() {
    val backStack = rememberNavBackStack(Route.EffectsList)
    val context = LocalContext.current
    val settingsRepository = remember(context) {
        AppSettingsRepository(context)
    }
    val settings by settingsRepository.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    // Wait for the stored appearance before showing the catalog, avoiding a flash of Classic.
    val currentSettings = settings ?: return

    val activeRoute = backStack.lastOrNull()
    if (activeRoute !is Route.Effect && activeRoute != Route.NavigationTestFeed) {
        AppSystemBars(darkIcons = currentSettings.appearance == AppAppearance.Neumorphism)
    }
    CompositionLocalProvider(LocalAppAppearance provides currentSettings.appearance) {
        Box(modifier = Modifier.fillMaxSize()) {
            PredictiveBackNavDisplay(
                backStack = backStack,
                selectedBackEffect = currentSettings.backEffect,
                onBackEffectSelected = { effect ->
                    scope.launch { settingsRepository.selectBackEffect(effect) }
                },
                selectedAppearance = currentSettings.appearance,
                onAppearanceSelected = { appearance ->
                    scope.launch { settingsRepository.selectAppearance(appearance) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
