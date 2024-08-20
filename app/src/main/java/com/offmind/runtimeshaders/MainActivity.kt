package com.offmind.runtimeshaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.offmind.runtimeshaders.screens.WashDownViewScreen
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuntimeShadersTheme {
                WashDownViewScreen()
            }
        }
    }
}