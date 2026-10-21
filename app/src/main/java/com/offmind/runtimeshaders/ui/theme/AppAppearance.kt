package com.offmind.runtimeshaders.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

internal enum class AppAppearance(val id: String, val title: String, val description: String) {
    Classic("classic", "Classic", "The original dark background and simple cards"),
    Neumorphism("neumorphism", "Neumorphism", "Soft raised surfaces and recessed controls"),
    Glass("glass", "Glass", "Translucent panels with illuminated edges");

    // Classic keeps its original artwork; widget materials follow the device theme.
    fun usesDarkTheme(systemDarkTheme: Boolean): Boolean = this == Classic || systemDarkTheme

    companion object {
        val default = Classic

        fun fromId(id: String?): AppAppearance = entries.firstOrNull { it.id == id } ?: default
    }
}

internal val LocalAppAppearance = staticCompositionLocalOf { AppAppearance.default }

// Resolved per appearance, including nested picker previews.
internal val LocalAppDarkTheme = staticCompositionLocalOf { true }
