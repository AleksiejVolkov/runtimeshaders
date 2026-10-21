package com.offmind.runtimeshaders.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Route : NavKey {
    @Serializable
    data object EffectsList : Route()

    @Serializable
    data class Category(val categoryId: String) : Route()

    @Serializable
    data class Effect(val effectId: String) : Route()

    @Serializable
    data object Settings : Route()

    @Serializable
    data object AppearancePicker : Route()

    @Serializable
    data object BackEffectPicker : Route()

    @Serializable
    data object NavigationTestFeed : Route()
}
