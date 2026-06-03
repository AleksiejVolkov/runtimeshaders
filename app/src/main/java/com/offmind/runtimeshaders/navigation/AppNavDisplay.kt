gitpackage com.offmind.runtimeshaders.navigation

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
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackShaderState
import com.offmind.runtimeshaders.screens.AllEffectsListScreen
import com.offmind.runtimeshaders.screens.BackEffectPickerScreen
import com.offmind.runtimeshaders.screens.SettingsScreen
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

internal data class RouteCallbacks(
    val onEffectSelected: (Route) -> Unit,
    val onSettingsSelected: () -> Unit,
    val onChooseBackEffect: () -> Unit,
    val onBack: () -> Unit,
    val onBackEffectSelected: (PredictiveBackEffect) -> Unit,
    val selectedBackEffect: PredictiveBackEffect
)

@Composable
internal fun PredictiveBackNavDisplay(
    state: PredictiveBackShaderState,
    selectedBackEffect: PredictiveBackEffect,
    onBackEffectSelected: (PredictiveBackEffect) -> Unit,
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    val routeCallbacks = RouteCallbacks(
        onEffectSelected = { backStack.add(it) },
        onSettingsSelected = { backStack.add(Route.Settings) },
        onChooseBackEffect = { backStack.add(Route.BackEffectPicker) },
        onBack = { backStack.removeLastOrNull() },
        onBackEffectSelected = onBackEffectSelected,
        selectedBackEffect = selectedBackEffect
    )

    PredictiveBackShaderLayer(
        state = state,
        effect = selectedBackEffect,
        modifier = modifier
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            popTransitionSpec = { noNavDisplayTransition },
            predictivePopTransitionSpec = { noNavDisplayTransition },
            entryProvider = routeEntryProvider(routeCallbacks)
        )
    }
}

private fun routeEntryProvider(callbacks: RouteCallbacks) = entryProvider {
    routeContentEntry<Route.EffectsList>(callbacks)
    routeContentEntry<Route.Settings>(callbacks)
    routeContentEntry<Route.BackEffectPicker>(callbacks)
    routeContentEntry<Route.LampShadow>(callbacks)
    routeContentEntry<Route.Waveshock>(callbacks)
    routeContentEntry<Route.SnowedDialog>(callbacks)
    routeContentEntry<Route.TestShader>(callbacks)
    routeContentEntry<Route.TapePlaneTest>(callbacks)
    routeContentEntry<Route.CircleTimer>(callbacks)
    routeContentEntry<Route.CanvasDeform>(callbacks)
    routeContentEntry<Route.Metaballs>(callbacks)
    routeContentEntry<Route.ColorfulToggle>(callbacks)
    routeContentEntry<Route.NavigationTest>(callbacks)
    routeContentEntry<Route.NavigationTestFeed>(callbacks)
}

private inline fun <reified T : Route> EntryProviderScope<NavKey>.routeContentEntry(
    callbacks: RouteCallbacks
) {
    entry<T> { route ->
        RouteContent(
            route = route,
            callbacks = callbacks
        )
    }
}

@Composable
internal fun RouteContent(
    route: Route,
    callbacks: RouteCallbacks
) {
    when (route) {
        is Route.EffectsList -> AllEffectsListScreen(
            effects = effectsCatalog,
            onEffectSelected = callbacks.onEffectSelected,
            onSettingsSelected = callbacks.onSettingsSelected
        )

        is Route.Settings -> SettingsScreen(
            onBack = callbacks.onBack,
            onChooseBackEffect = callbacks.onChooseBackEffect
        )

        is Route.BackEffectPicker -> BackEffectPickerScreen(
            selectedEffect = callbacks.selectedBackEffect,
            onBack = callbacks.onBack,
            onEffectSelected = callbacks.onBackEffectSelected
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
                callbacks.onEffectSelected(
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
