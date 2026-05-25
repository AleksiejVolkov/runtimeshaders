package com.offmind.runtimeshaders.navigation.predictive

internal enum class PredictiveBackEffect(
    val id: String,
    val title: String,
    val description: String,
    val shaderSource: String
) {
    HumpMask(
        id = "hump_mask",
        title = "Hump mask",
        description = "Soft edge pull deformation from the swipe side.",
        shaderSource = predictiveBackHumpMaskShader
    ),
    ParticleDissolve(
        id = "particle_dissolve",
        title = "Particle dissolve",
        description = "Center-out particle dissolve with subtle motion.",
        shaderSource = predictiveBackParticleDissolveShader
    ),
    LiquidDrain(
        id = "liquid_drain",
        title = "Liquid drain",
        description = "Current screen softens and drains downward like liquid.",
        shaderSource = predictiveBackLiquidDrainShader
    );

    companion object {
        val default = ParticleDissolve
        val entriesList = entries.toList()

        fun fromId(id: String?): PredictiveBackEffect {
            return entries.firstOrNull { it.id == id } ?: default
        }
    }
}
