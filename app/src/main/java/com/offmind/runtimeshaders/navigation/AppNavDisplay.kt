package com.offmind.runtimeshaders.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.offmind.runtimeshaders.navigation.predictive.BACK_CANCEL_DURATION_MS
import com.offmind.runtimeshaders.navigation.predictive.BACK_COMPLETE_DURATION_MS
import com.offmind.runtimeshaders.navigation.predictive.BACK_COMPLETE_PROGRESS
import com.offmind.runtimeshaders.navigation.predictive.LocalPredictiveBackRender
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackDissolve
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackRenderState
import com.offmind.runtimeshaders.screens.AllEffectsListScreen
import com.offmind.runtimeshaders.screens.BackEffectPickerScreen
import com.offmind.runtimeshaders.screens.SettingsScreen
import com.offmind.runtimeshaders.screens.effects.CanvasDeformScreen
import com.offmind.runtimeshaders.screens.effects.ColorfulToggleScreen
import com.offmind.runtimeshaders.screens.effects.DualLightTextureScreen
import com.offmind.runtimeshaders.screens.effects.IlluminateUiScreen
import com.offmind.runtimeshaders.screens.effects.LampWithShadowScreen
import com.offmind.runtimeshaders.screens.effects.MetaballsShaderScreen
import com.offmind.runtimeshaders.screens.effects.NavigationTestFeedScreen
import com.offmind.runtimeshaders.screens.effects.NavigationTestScreen
import com.offmind.runtimeshaders.screens.effects.NeonFogButtonScreen
import com.offmind.runtimeshaders.screens.effects.SnowDialogScreen
import com.offmind.runtimeshaders.screens.effects.TapePlaneTestScreen
import com.offmind.runtimeshaders.screens.effects.TestShaderScreen
import com.offmind.runtimeshaders.screens.effects.TimerShaderScreen
import com.offmind.runtimeshaders.screens.effects.WaveshockOnTapScreen
import com.offmind.runtimeshaders.screens.effectsCatalog
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.launch

internal data class RouteCallbacks(
    val onEffectSelected: (Route) -> Unit,
    val onSettingsSelected: () -> Unit,
    val onChooseBackEffect: () -> Unit,
    val onBack: () -> Unit,
    val onBackEffectSelected: (PredictiveBackEffect) -> Unit,
    val selectedBackEffect: PredictiveBackEffect
)

/**
 * Predictive-back navigation driven single-source.
 *
 * The app owns the gesture (one [NavigationBackHandler]) and feeds the *same*
 * [androidx.navigationevent.compose.NavigationEventState] into the low-level [NavDisplay]
 * overload. That lets NavDisplay render the **real** destination entry behind the outgoing one
 * during the gesture (so its scroll/text/etc. are correct), while we read the same gesture state
 * to drive the dissolve shader — which is applied per-entry to only the outgoing screen.
 *
 * The shader's full dissolve completes at progress = [BACK_COMPLETE_PROGRESS], reached exactly as
 * the pop commits, so there is no post-commit overshoot fighting NavDisplay's lifecycle.
 */
@Composable
internal fun PredictiveBackNavDisplay(
    backStack: NavBackStack<NavKey>,
    selectedBackEffect: PredictiveBackEffect,
    onBackEffectSelected: (PredictiveBackEffect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val callbacks = RouteCallbacks(
        onEffectSelected = { backStack.add(it) },
        onSettingsSelected = { backStack.add(Route.Settings) },
        onChooseBackEffect = { backStack.add(Route.BackEffectPicker) },
        onBack = { backStack.removeLastOrNull() },
        onBackEffectSelected = onBackEffectSelected,
        selectedBackEffect = selectedBackEffect,
    )

    // Decorated real entries + scene state — this is what lets NavDisplay show the actual
    // destination (with its preserved state) behind the outgoing screen during the gesture.
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = routeEntryProvider(callbacks),
    )
    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategy = SinglePaneSceneStrategy(),
        onBack = { backStack.removeLastOrNull() },
    )
    val scene = sceneState.currentScene
    val gestureState = rememberNavigationEventState(
        currentInfo = SceneInfo(scene),
        backInfo = sceneState.previousScenes.map { SceneInfo(it) },
    )

    // ── Gesture → shader progress/touch/edge ──────────────────────────────────────────────
    val terminalProgress = remember { Animatable(0f) }          // commit/cancel settle (0..1)
    var startTouchY by remember { mutableStateOf<Float?>(null) }
    var lastTouch by remember { mutableStateOf(Offset.Zero) }
    var lastEdge by remember { mutableIntStateOf(NavigationEvent.EDGE_NONE) }
    var outgoingKey by remember { mutableStateOf<Any?>(null) }
    var lastGestureProgress by remember { mutableStateOf(0f) }

    val inProgress = gestureState.transitionState as? NavigationEventTransitionState.InProgress
    val latestEvent = inProgress?.latestEvent
    if (latestEvent != null) {
        if (startTouchY == null) startTouchY = latestEvent.touchY
        if (outgoingKey == null) outgoingKey = backStack.lastOrNull()
        lastTouch = Offset(
            x = if (latestEvent.swipeEdge == NavigationEvent.EDGE_RIGHT) Float.POSITIVE_INFINITY else 0f,
            y = startTouchY ?: latestEvent.touchY,
        )
        lastEdge = latestEvent.swipeEdge
        lastGestureProgress = latestEvent.progress
    }

    val rawProgress = maxOf(latestEvent?.progress ?: 0f, terminalProgress.value)
    val renderState = PredictiveBackRenderState(
        active = rawProgress > 0f,
        progress = rawProgress * BACK_COMPLETE_PROGRESS,
        touch = lastTouch,
        edge = lastEdge,
        effect = selectedBackEffect,
        outgoingKey = outgoingKey,
    )

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCancelled = {
            scope.launch {
                terminalProgress.animateTo(0f, tween(BACK_CANCEL_DURATION_MS))
                terminalProgress.snapTo(0f)
                startTouchY = null
                outgoingKey = null
            }
        },
        onBackCompleted = {
            scope.launch {
                terminalProgress.snapTo(lastGestureProgress)
                backStack.removeLastOrNull()                 // commit — NavDisplay reveals destination
                terminalProgress.animateTo(1f, tween(BACK_COMPLETE_DURATION_MS))
                terminalProgress.snapTo(0f)
                startTouchY = null
                outgoingKey = null
            }
        },
    )

    CompositionLocalProvider(LocalPredictiveBackRender provides renderState) {
        NavDisplay(
            sceneState = sceneState,
            navigationEventState = gestureState,
            modifier = modifier,
            // Hold both screens fully opaque for the dissolve duration; the shader does the visual.
            popTransitionSpec = holdTransform,
            predictivePopTransitionSpec = { holdTransform(this) },
        )
    }
}

// Keeps outgoing + incoming fully visible for the dissolve window so the per-entry shader, not a
// built-in fade, performs the transition. (alpha held at 1, just used to define the duration.)
private val holdTransform: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    fadeIn(tween(BACK_COMPLETE_DURATION_MS), initialAlpha = 1f) togetherWith
        fadeOut(tween(BACK_COMPLETE_DURATION_MS), targetAlpha = 1f)
}

private fun routeEntryProvider(callbacks: RouteCallbacks): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
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
        routeContentEntry<Route.NeonFogButton>(callbacks)
        routeContentEntry<Route.DualLightTexture>(callbacks)
        routeContentEntry<Route.NavigationTest>(callbacks)
        routeContentEntry<Route.NavigationTestFeed>(callbacks)
        routeContentEntry<Route.IlluminateUi>(callbacks)
    }

private inline fun <reified T : Route> EntryProviderScope<NavKey>.routeContentEntry(
    callbacks: RouteCallbacks
) {
    entry<T> { route ->
        PredictiveBackDissolve(entryKey = route) {
            RouteContent(route = route, callbacks = callbacks)
        }
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
        is Route.NeonFogButton -> NeonFogButtonScreen()
        is Route.DualLightTexture -> DualLightTextureScreen()
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
        is Route.IlluminateUi -> IlluminateUiScreen()
    }
}
