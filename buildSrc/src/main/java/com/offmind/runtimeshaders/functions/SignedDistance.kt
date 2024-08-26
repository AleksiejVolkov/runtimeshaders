package com.offmind.runtimeshaders.functions

/**
 * A function that calculates the Signed Distance Function (SDF) for a circle.
 *
 * This function computes the distance from a point `p` to the edge of a circle with radius `r`.
 * The distance is positive if the point is outside the circle, negative if inside, and zero if on the edge.
 *
 * @param p The 2D vector representing the point from which the distance is calculated.
 * @param r The radius of the circle.
 * @return The signed distance from the point `p` to the edge of the circle.
 */
val CircleSDFFunction = """
    float CircleSDF(vec2 p, float r) {
        return length(p) - r;
    }   
""".trimIndent()

val allSdfFunctions = mapOf(
    "CircleSDF" to CircleSDFFunction
)
