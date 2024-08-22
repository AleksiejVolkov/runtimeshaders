package com.offmind.runtimeshaders.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    class LampShadow(val title: String, val description: String) : Route()

    @Serializable
    class Waveshock(val title: String, val description: String) : Route()

    @Serializable
    data object EffectsList : Route()
}
