package com.offmind.runtimeshaders.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackShaderLayer
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackShaderState
import com.offmind.runtimeshaders.screens.AllEffectsListScreen
import com.offmind.runtimeshaders.screens.effects.CanvasDeformScreen
import com.offmind.runtimeshaders.screens.effects.ColorfulToggleScreen
import com.offmind.runtimeshaders.screens.effects.LampWithShadowScreen
import com.offmind.runtimeshaders.screens.effects.MetaballsShaderScreen
import com.offmind.runtimeshaders.screens.effects.NavigationTestFeedScreen
import com.offmind.runtimeshaders.screens.effects.NavigationTestScreen
import com.offmind.runtimeshaders.screens.effects.SnowDialogScreen
import com.offmind.runtimeshaders.screens.effects.TapePlaneTestScreen
import com.offmind.runtimeshaders.screens.effects.TestShaderScreen
import com.offmind.runtimeshaders.screens.effects.TimerShaderScreen
import com.offmind.runtimeshaders.screens.effects.WaveshockOnTapScreen
import com.offmind.runtimeshaders.screens.effectsCatalog

@Composable
internal fun PredictiveBackNavDisplay(
    state: PredictiveBackShaderState,
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    val onEffectSelected: (Route) -> Unit = { backStack.add(it) }

    PredictiveBackShaderLayer(
        state = state,
        modifier = modifier
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            popTransitionSpec = { noNavDisplayTransition },
            predictivePopTransitionSpec = { noNavDisplayTransition },
            entryProvider = routeEntryProvider(onEffectSelected)
        )
    }
}

private fun routeEntryProvider(
    onEffectSelected: (Route) -> Unit
) = entryProvider {
    routeContentEntry<Route.EffectsList>(onEffectSelected)
    routeContentEntry<Route.LampShadow>(onEffectSelected)
    routeContentEntry<Route.Waveshock>(onEffectSelected)
    routeContentEntry<Route.SnowedDialog>(onEffectSelected)
    routeContentEntry<Route.TestShader>(onEffectSelected)
    routeContentEntry<Route.TapePlaneTest>(onEffectSelected)
    routeContentEntry<Route.CircleTimer>(onEffectSelected)
    routeContentEntry<Route.CanvasDeform>(onEffectSelected)
    routeContentEntry<Route.Metaballs>(onEffectSelected)
    routeContentEntry<Route.ColorfulToggle>(onEffectSelected)
    routeContentEntry<Route.NavigationTest>(onEffectSelected)
    routeContentEntry<Route.NavigationTestFeed>(onEffectSelected)
}

private inline fun <reified T : Route> EntryProviderScope<NavKey>.routeContentEntry(
    noinline onEffectSelected: (Route) -> Unit
) {
    entry<T> { route ->
        RouteContent(
            route = route,
            onEffectSelected = onEffectSelected
        )
    }
}

@Composable
internal fun RouteContent(
    route: Route,
    onEffectSelected: (Route) -> Unit
) {
    when (route) {
        is Route.EffectsList -> AllEffectsListScreen(
            effects = effectsCatalog,
            onEffectSelected = onEffectSelected
        )

        is Route.LampShadow -> LampWithShadowScreen()
        is Route.Waveshock -> WaveshockOnTapScreen()
        is Route.SnowedDialog -> SnowDialogScreen()
        is Route.TestShader -> TestShaderScreen()
        is Route.TapePlaneTest -> TapePlaneTestScreen()
        is Route.CircleTimer -> TimerShaderScreen()
        is Route.CanvasDeform -> CanvasDeformScreen()
        is Route.Metaballs -> MetaballsShaderScreen()
        is Route.ColorfulToggle -> ColorfulToggleScreen()
        is Route.NavigationTest -> NavigationTestScreen(
            onLogin = {
                onEffectSelected(
                    Route.NavigationTestFeed(
                        "Navigation Test Feed",
                        "Mock feed"
                    )
                )
            }
        )
        is Route.NavigationTestFeed -> NavigationTestFeedScreen()
    }
}

private val noNavDisplayTransition = ContentTransform(
    targetContentEnter = EnterTransition.None,
    initialContentExit = ExitTransition.None
)
