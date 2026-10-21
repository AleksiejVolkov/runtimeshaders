package com.offmind.runtimeshaders.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.navigation3.runtime.NavBackStack
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
import com.offmind.runtimeshaders.screens.AppearancePickerScreen
import com.offmind.runtimeshaders.screens.BackEffectPickerScreen
import com.offmind.runtimeshaders.screens.EffectCatalogItem
import com.offmind.runtimeshaders.screens.EffectCategory
import com.offmind.runtimeshaders.screens.EffectScreenData
import com.offmind.runtimeshaders.screens.EffectsCatalogScreen
import com.offmind.runtimeshaders.screens.SettingsScreen
import com.offmind.runtimeshaders.screens.effects.NavigationTestFeedScreen
import com.offmind.runtimeshaders.screens.effectsCatalog
import com.offmind.runtimeshaders.ui.theme.AppAppearance
import kotlinx.coroutines.launch

internal data class RouteCallbacks(
    val onNavigate: (Route) -> Unit,
    val onSettingsSelected: () -> Unit,
    val onChooseBackEffect: () -> Unit,
    val onChooseAppearance: () -> Unit,
    val onAppearanceSelected: (AppAppearance) -> Unit,
    val selectedAppearance: AppAppearance,
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
    selectedAppearance: AppAppearance,
    onAppearanceSelected: (AppAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val callbacks = RouteCallbacks(
        onNavigate = { backStack.add(it) },
        onSettingsSelected = { backStack.add(Route.Settings) },
        onChooseBackEffect = { backStack.add(Route.BackEffectPicker) },
        onChooseAppearance = { backStack.add(Route.AppearancePicker) },
        onAppearanceSelected = onAppearanceSelected,
        selectedAppearance = selectedAppearance,
        onBack = { backStack.removeLastOrNull() },
        onBackEffectSelected = onBackEffectSelected,
        selectedBackEffect = selectedBackEffect,
    )
    // Nav entries can retain their original content lambda while settings change. Read the latest
    // callbacks inside that lambda so already-open pickers update their selected option as well.
    val currentCallbacks = rememberUpdatedState(callbacks)

    // Decorated real entries + scene state — this is what lets NavDisplay show the actual
    // destination (with its preserved state) behind the outgoing screen during the gesture.
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = routeEntryProvider(currentCallbacks),
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

private fun routeEntryProvider(callbacks: State<RouteCallbacks>): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        routeContentEntry<Route.EffectsList>(callbacks)
        routeContentEntry<Route.Settings>(callbacks)
        routeContentEntry<Route.BackEffectPicker>(callbacks)
        routeContentEntry<Route.AppearancePicker>(callbacks)
        routeContentEntry<Route.Category>(callbacks)
        routeContentEntry<Route.Effect>(callbacks)
        routeContentEntry<Route.NavigationTestFeed>(callbacks)
    }

private inline fun <reified T : Route> EntryProviderScope<NavKey>.routeContentEntry(
    callbacks: State<RouteCallbacks>
) {
    entry<T> { route ->
        PredictiveBackDissolve(entryKey = route) {
            RouteContent(route = route, callbacks = callbacks.value)
        }
    }
}

@Composable
internal fun RouteContent(
    route: Route,
    callbacks: RouteCallbacks
) {
    val onItemSelected: (EffectCatalogItem) -> Unit = { item ->
        callbacks.onNavigate(
            when (item) {
                is EffectCategory -> Route.Category(item.id)
                is EffectScreenData -> Route.Effect(item.id)
            }
        )
    }
    when (route) {
        Route.EffectsList, is Route.Category -> {
            val category = if (route is Route.Category) {
                effectsCatalog.category(route.categoryId)
            } else {
                effectsCatalog.root
            }
            EffectsCatalogScreen(
                category = category,
                ancestors = effectsCatalog.ancestors(category.id),
                onItemSelected = onItemSelected,
                onSettingsSelected = callbacks.onSettingsSelected,
                onBack = if (route is Route.Category) callbacks.onBack else null,
            )
        }

        is Route.Effect -> effectsCatalog.effect(route.effectId).content(callbacks.onNavigate)

        Route.Settings -> SettingsScreen(
            onBack = callbacks.onBack,
            onChooseBackEffect = callbacks.onChooseBackEffect,
            onChooseAppearance = callbacks.onChooseAppearance,
            selectedAppearance = callbacks.selectedAppearance,
            selectedBackEffect = callbacks.selectedBackEffect,
        )

        Route.BackEffectPicker -> BackEffectPickerScreen(
            selectedEffect = callbacks.selectedBackEffect,
            onBack = callbacks.onBack,
            onEffectSelected = callbacks.onBackEffectSelected
        )

        Route.AppearancePicker -> AppearancePickerScreen(
            selectedAppearance = callbacks.selectedAppearance,
            onBack = callbacks.onBack,
            onAppearanceSelected = callbacks.onAppearanceSelected,
        )

        Route.NavigationTestFeed -> NavigationTestFeedScreen()
    }
}
