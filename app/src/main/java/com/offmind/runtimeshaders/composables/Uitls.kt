package com.offmind.runtimeshaders.composables

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun provideTimeAsState(initialValue: Float = 0f): State<Float> {
    val timeState = remember { mutableFloatStateOf(initialValue) }

    LaunchedEffect(Unit) {
        while (true) {
            timeState.floatValue += 0.01f
            delay(10)
        }
    }

    return timeState
}