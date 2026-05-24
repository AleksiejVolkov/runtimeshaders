package com.offmind.runtimeshaders

import androidx.compose.runtime.Composable
import com.offmind.runtimeshaders.navigation.RuntimeShadersNavHost
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme

@Composable
internal fun RuntimeShadersApp() {
    RuntimeShadersTheme {
        RuntimeShadersNavHost()
    }
}
