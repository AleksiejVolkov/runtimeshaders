package com.offmind.runtimeshaders.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    class LampShadow(val title: String, val description: String) : Route()

    @Serializable
    class Waveshock(val title: String, val description: String) : Route()

    @Serializable
    class TestShader(val title: String, val description: String) : Route()

    @Serializable
    class TapePlaneTest(val title: String, val description: String) : Route()

    @Serializable
    data class CircleTimer(val title: String, val description: String) : Route()

    @Serializable
    data object EffectsList : Route()

    @Serializable
    data class SnowedDialog(val title: String, val description: String) : Route()

    @Serializable
    data class CanvasDeform(val title: String, val description: String) : Route()

    @Serializable
    data class Metaballs(val title: String, val description: String) : Route()

    @Serializable
    data class ColorfulToggle(val title: String, val description: String) : Route()
}
