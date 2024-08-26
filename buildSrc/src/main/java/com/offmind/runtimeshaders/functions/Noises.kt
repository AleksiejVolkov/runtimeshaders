package com.offmind.runtimeshaders.functions

/**
 * A function that generates 2D Simplex noise.
 *
 * This function computes a noise value based on the input vector `p` using a combination of hash functions
 * and cosine interpolation. The result is a float value representing the noise at the given position.
 *
 * @param p The 2D vector input used to generate the noise value.
 * @return A float value representing the noise at the given position.
 */
val SimplexNoiseFunction = """
    float SNoise(vec2 p) {
        float unit = 0.05;
        vec2 ij = floor(p/unit);
        vec2 xy = mod(p,unit)/unit;
        xy = .5*(1.-cos(3.1415*xy));
        float a = Hash21((ij+vec2(0.,0.)));
        float b = Hash21((ij+vec2(1.,0.)));
        float c = Hash21((ij+vec2(0.,1.)));
        float d = Hash21((ij+vec2(1.,1.)));
        float x1 = mix(a, b, xy.x);
        float x2 = mix(c, d, xy.x);
        return mix(x1, x2, xy.y);
    }
""".trimIndent()

val NoiseFunction = """
    // Based on Morgan McGuire @morgan3d
    // https://www.shadertoy.com/view/4dS3Wd
    float Noise(in vec2 st) {
        vec2 i = floor(st);
        vec2 f = fract(st);
    
        // Four corners in 2D of a tile
        float a = Hash21(i);
        float b = Hash21(i + vec2(1.0, 0.0));
        float c = Hash21(i + vec2(0.0, 1.0));
        float d = Hash21(i + vec2(1.0, 1.0));
    
        vec2 u = f * f * (3.0 - 2.0 * f);
    
        return mix(a, b, u.x) +
                (c - a)* u.y * (1.0 - u.x) +
                (d - b) * u.x * u.y;
}
""".trimIndent()

val FBMFunction = """
    float FBM(in vec2 st, int octaves) {
        // Initial values
        float value = 0.0;
        float amplitude = 0.5;
        float frequency = 1.0;
        const int MAX_OCTAVES = 10;  
        
        for (int i = 0; i < MAX_OCTAVES; i++) { 
            if (i >= octaves) break; 
            value += amplitude * Noise(st * frequency);
            frequency *= 2.0;
            amplitude *= 0.5;
        }   
    return value;
}
""".trimIndent()

val allNoisesFunctions = mapOf(
    "SimplexNoise" to SimplexNoiseFunction,
    "Noise" to NoiseFunction,
    "FBM" to FBMFunction
)
