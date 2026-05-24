package com.offmind.runtimeshaders.navigation.predictive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventState

data class PredictiveBackShaderState internal constructor(
    val progress: Float,
    val touch: Offset,
    val swipeEdge: Int
)

@Composable
internal fun NavigationEventState<NavigationEventInfo.None>.toShaderState(
    resetToken: Int
): PredictiveBackShaderState {
    val transitionState = transitionState
    val latestEvent = (transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent
    val startTouchY = remember { mutableStateOf<Float?>(null) }
    val lastTouch = remember { mutableStateOf(Offset.Zero) }
    val lastSwipeEdge = remember { mutableIntStateOf(NavigationEvent.EDGE_NONE) }

    LaunchedEffect(resetToken) {
        startTouchY.value = null
        lastTouch.value = Offset.Zero
        lastSwipeEdge.intValue = NavigationEvent.EDGE_NONE
    }

    if (latestEvent != null && startTouchY.value == null) {
        startTouchY.value = latestEvent.touchY
    }
    if (latestEvent != null) {
        val touch = Offset(
            x = when (latestEvent.swipeEdge) {
                NavigationEvent.EDGE_RIGHT -> Float.POSITIVE_INFINITY
                else -> 0f
            },
            y = startTouchY.value ?: latestEvent.touchY
        )
        lastTouch.value = touch
        lastSwipeEdge.intValue = latestEvent.swipeEdge
    }

    return PredictiveBackShaderState(
        progress = latestEvent?.progress ?: 0f,
        touch = lastTouch.value,
        swipeEdge = lastSwipeEdge.intValue
    )
}
