package com.offmind.runtimeshaders.functions

val BackInOutFunction = """
    float BackInOut(float t) {
        float PI = 3.141592653589793;
        float f = t < 0.5
        ? 2.0 * t
        : 1.0 - (2.0 * t - 1.0);
    
        float g = pow(f, 3.0) - f * sin(f * PI);
    
        return t < 0.5
        ? 0.5 * g
        : 0.5 * (1.0 - g) + 0.5;
    }
""".trimIndent()

val BackInFunction = """
    float BackIn(float t) {
      float PI = 3.141592653589793;
      return pow(t, 3.0) - t * sin(t * PI);
    }
""".trimIndent()

val BackOutFunction = """
    float BackOut(float t) {
      float PI = 3.141592653589793;
      float f = 1.0 - t;
      return 1.0 - (pow(f, 3.0) - f * sin(f * PI));
    }
""".trimIndent()

val BounceInOutFunction = """
    float BounceInOut(float t) {
      return t < 0.5
        ? 0.5 * (1.0 - BounceOut(1.0 - t * 2.0))
        : 0.5 * BounceOut(t * 2.0 - 1.0) + 0.5;
    }
""".trimIndent()

val BounceInFunction = """
    float BounceIn(float t) {
      return 1.0 - BounceOut(1.0 - t);
    }
""".trimIndent()

val BounceOutFunction = """
    float BounceOut(float t) {
      float PI = 1.5707963267948966;
      const float a = 4.0 / 11.0;
      const float b = 8.0 / 11.0;
      const float c = 9.0 / 10.0;

      const float ca = 4356.0 / 361.0;
      const float cb = 35442.0 / 1805.0;
      const float cc = 16061.0 / 1805.0;

      float t2 = t * t;

      return t < a
        ? 7.5625 * t2
        : t < b
          ? 9.075 * t2 - 9.9 * t + 3.4
          : t < c
            ? ca * t2 - cb * t + cc
            : 10.8 * t * t - 20.52 * t + 10.72;
    }
""".trimIndent()

val CircularInOut = """
    float CircularInOut(float t) {
      return t < 0.5
        ? 0.5 * (1.0 - sqrt(1.0 - 4.0 * t * t))
        : 0.5 * (sqrt((3.0 - 2.0 * t) * (2.0 * t - 1.0)) + 1.0);
    }
""".trimIndent()

val CircularIn = """
    float CircularIn(float t) {
      return 1.0 - sqrt(1.0 - t * t);
    }
""".trimIndent()

val CircularOut = """
    float CircularOut(float t) {
      return sqrt((2.0 - t) * t);
    }
""".trimIndent()

val CubicInOut = """
    float CubicInOut(float t) {
      return t < 0.5
        ? 4.0 * t * t * t
        : 0.5 * pow(2.0 * t - 2.0, 3.0) + 1.0;
    }
""".trimIndent()

val CubicIn = """
    float CubicIn(float t) {
      return t * t * t;
    }
""".trimIndent()

val CubicOut = """
    float CubicOut(float t) {
      float f = t - 1.0;
      return f * f * f + 1.0;
    }
""".trimIndent()

val ElasticInOut = """
    float ElasticInOut(float t) {
      float HALF_PI = 1.5707963267948966;
      return t < 0.5
    ? 0.5 * sin(+13.0 * HALF_PI * 2.0 * t) * pow(2.0, 10.0 * (2.0 * t - 1.0))
    : 0.5 * sin(-13.0 * HALF_PI * ((2.0 * t - 1.0) + 1.0)) * pow(2.0, -10.0 * (2.0 * t - 1.0)) + 1.0;
    }
""".trimIndent()

val ElasticIn = """
    float ElasticIn(float t) {
      float HALF_PI = 1.5707963267948966;
      return sin(13.0 * t * HALF_PI) * pow(2.0, 10.0 * (t - 1.0));
    }
""".trimIndent()

val ElasticOut = """
    float ElasticOut(float t) {
      float HALF_PI = 1.5707963267948966;
      return sin(-13.0 * (t + 1.0) * HALF_PI) * pow(2.0, -10.0 * t) + 1.0;
    }
""".trimIndent()

val ExponentialInOut = """
    float ExponentialInOut(float t) {
      return t == 0.0 || t == 1.0
        ? t
        : t < 0.5
          ? +0.5 * pow(2.0, (20.0 * t) - 10.0)
          : -0.5 * pow(2.0, 10.0 - (t * 20.0)) + 1.0;
    }
""".trimIndent()

val ExponentialIn = """
    float ExponentialIn(float t) {
      return t == 0.0 ? t : pow(2.0, 10.0 * (t - 1.0));
    }
""".trimIndent()

val ExponentialOut = """
    float ExponentialOut(float t) {
      return t == 1.0 ? t : 1.0 - pow(2.0, -10.0 * t);
    }
""".trimIndent()

val Linear = """
    float Linear(float t) {
      return t;
    }
""".trimIndent()

val QuadraticInOut = """
    float QuadraticInOut(float t) {
      return t < 0.5
        ? 2.0 * t * t
        : -1.0 + (4.0 - 2.0 * t) * t;
    }
""".trimIndent()

val QuadraticIn = """
    float QuadraticIn(float t) {
      return t * t;
    }
""".trimIndent()

val QuadraticOut = """
    float QuadraticOut(float t) {
      return -t * (t - 2.0);
    }
""".trimIndent()

val allEasingFunctions = mapOf(
    "BackInOut" to BackInOutFunction,
    "BackIn" to BackInFunction,
    "BackOut" to BackOutFunction,
    "BounceInOut" to BounceInOutFunction,
    "BounceIn" to BounceInFunction,
    "BounceOut" to BounceOutFunction,
    "CircularInOut" to CircularInOut,
    "CircularIn" to CircularIn,
    "CircularOut" to CircularOut,
    "CubicInOut" to CubicInOut,
    "CubicIn" to CubicIn,
    "CubicOut" to CubicOut,
    "ElasticInOut" to ElasticInOut,
    "ElasticIn" to ElasticIn,
    "ElasticOut" to ElasticOut,
    "ExponentialInOut" to ExponentialInOut,
    "ExponentialIn" to ExponentialIn,
    "ExponentialOut" to ExponentialOut,
    "Linear" to Linear,
    "QuadraticInOut" to QuadraticInOut,
    "QuadraticIn" to QuadraticIn,
    "QuadraticOut" to QuadraticOut
)