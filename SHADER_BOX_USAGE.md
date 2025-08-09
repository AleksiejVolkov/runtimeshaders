# ShaderBox Composable

The `ShaderBox` is a Compose UI component that lets developers apply rich shader effects to any content within a `Box`. This component is particularly useful for achieving custom graphics and animations using AGSL (Android Graphics Shading Language).

## Features
- **Shader Effects**: Apply custom shading logic using AGSL via the `Shader` API.
- **Uniform Control**: Pass uniform data to your shaders using `ShaderTypedValue`.
- **Resolution and Time Control**: Automatically manages resolution and time uniforms for animations.
- **Composable Content**: Allows integration of complex UI components.
- **Highly Customizable**: Works with various types of uniform data like float, vectors, etc.

## How to Use

### Import the Dependency
Ensure you have the necessary shader utilities in your project setup:
```kotlin
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
```

### Basic Usage
To create a simple shader effect using `ShaderBox`, pass a `RuntimeShader` and optionally define uniforms:

```kotlin
@Composable
fun ShaderExampleScreen() {
    val myShader = remember {
        Shader("your-shader-code-here").getRuntimeShader()
    }

    ShadedBox(
        shader = myShader,
        shaderUniforms = mapOf(
            "myUniform" to ShaderTypedValue.FloatType(1.0f), // Example uniform
        )
    ) {
        // Place any composable content here.
    }
}
```

### Shader Uniforms
`ShaderBox` allows customization of the shader with various uniform types:
- **Float**: `ShaderTypedValue.FloatType(value: Float)`
- **Vec2**: `ShaderTypedValue.Vec2Type(x: Float, y: Float)`
- **Vec3**: `ShaderTypedValue.Vec3Type(x: Float, y: Float, z: Float)`
- **Vec4**: `ShaderTypedValue.Vec4Type(x: Float, y: Float, z: Float, w: Float)`

### Time and Resolution Management
Built-in management of time and resolution can be enabled:

```kotlin
ShadedBox(
    shader = myShader,
    includeTime = true,  // Automatically handle the "time" uniform
    shaderUniforms = mapOf("resolution" to ShaderTypedValue.Vec2Type(1080f, 1920f))
) {
    // Composable UI
}
```

### Example Use Cases
1. **Animated Backgrounds**: Using dynamic shaders to create animations as the background.
2. **Interactive Effects**: Shader effects that respond to user interaction, such as pointer-based distortions.
3. **Artistic Touches**: Enhance images or UI components with visual style through shaders.

### Advanced Capabilities
With the ShaderBox, you can include any shader logic compatible with AGSL. Use custom shader functions from dependencies included in your `Shader` setup, leveraging a variety of predefined visual effects.

By managing shader code and configuration within the Compose framework, `ShaderBox` seamlessly integrates into the UI ecosystem with ease while providing powerful graphical effects.
