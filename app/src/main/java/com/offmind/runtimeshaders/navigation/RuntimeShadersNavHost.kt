package com.offmind.runtimeshaders.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.rememberNavBackStack
import com.offmind.runtimeshaders.data.BackEffectSettingsRepository
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import kotlinx.coroutines.launch

@Composable
internal fun RuntimeShadersNavHost() {
    val backStack = rememberNavBackStack(Route.EffectsList)
    val context = LocalContext.current
    val settingsRepository = remember(context) {
        BackEffectSettingsRepository(context)
    }
    val selectedBackEffect by settingsRepository.selectedEffect.collectAsState(
        initial = PredictiveBackEffect.default
    )
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        PredictiveBackNavDisplay(
            backStack = backStack,
            selectedBackEffect = selectedBackEffect,
            onBackEffectSelected = { effect ->
                scope.launch { settingsRepository.selectEffect(effect) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
