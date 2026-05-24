package com.offmind.runtimeshaders.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.offmind.runtimeshaders.navigation.predictive.BACK_CANCEL_DURATION_MS
import com.offmind.runtimeshaders.navigation.predictive.BACK_COMPLETE_DURATION_MS
import com.offmind.runtimeshaders.navigation.predictive.BACK_COMPLETE_PROGRESS
import com.offmind.runtimeshaders.navigation.predictive.BACK_POP_DELAY_MS
import com.offmind.runtimeshaders.navigation.predictive.BACK_RESET_FRAME_DELAY
import com.offmind.runtimeshaders.navigation.predictive.toShaderState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun RuntimeShadersNavHost() {
    val backStack = rememberNavBackStack(Route.EffectsList)

    Box(modifier = Modifier.fillMaxSize()) {
        val backEventState = rememberNavigationEventState(
            currentInfo = NavigationEventInfo.None,
            backInfo = if (backStack.size > 1) {
                listOf(NavigationEventInfo.None)
            } else {
                emptyList()
            }
        )
        var gestureSession by remember { mutableIntStateOf(0) }
        var activeUnderlayRoute by remember { mutableStateOf<Route?>(null) }
        val terminalProgress = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val gestureShaderState = backEventState.toShaderState(resetToken = gestureSession)

        LaunchedEffect(gestureShaderState.progress) {
            if (gestureShaderState.progress > 0f) {
                if (activeUnderlayRoute == null) {
                    activeUnderlayRoute = backStack.dropLast(1).lastOrNull() as? Route
                }
                terminalProgress.snapTo(gestureShaderState.progress)
            }
        }

        val shaderState = gestureShaderState.copy(
            progress = maxOf(gestureShaderState.progress, terminalProgress.value)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            val underlayRoute = if (shaderState.progress > 0f) {
                activeUnderlayRoute ?: backStack.dropLast(1).lastOrNull() as? Route
            } else {
                null
            }

            if (underlayRoute != null) {
                RouteContent(
                    route = underlayRoute,
                    onEffectSelected = { backStack.add(it) }
                )
            }

            PredictiveBackNavDisplay(
                state = shaderState,
                backStack = backStack,
                modifier = Modifier.fillMaxSize()
            )
        }

        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = backStack.size > 1,
            onBackCancelled = {
                scope.launch {
                    terminalProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = BACK_CANCEL_DURATION_MS)
                    )
                    activeUnderlayRoute = null
                    gestureSession++
                }
            },
            onBackCompleted = {
                scope.launch {
                    terminalProgress.animateTo(
                        targetValue = BACK_COMPLETE_PROGRESS,
                        animationSpec = tween(durationMillis = BACK_COMPLETE_DURATION_MS)
                    )
                    delay(BACK_POP_DELAY_MS.milliseconds)
                    backStack.removeLastOrNull()
                    repeat(BACK_RESET_FRAME_DELAY) {
                        withFrameNanos { }
                    }
                    terminalProgress.snapTo(0f)
                    activeUnderlayRoute = null
                    gestureSession++
                }
            }
        )
    }
}
