package com.offmind.runtimeshaders.gl.geometry

import com.offmind.runtimeshaders.gl.GlRenderConstants

data class VertexLayout(
    val positionComponents: Int = GlRenderConstants.POSITION_COMPONENTS,
    val normalComponents: Int = GlRenderConstants.NORMAL_COMPONENTS,
    val colorComponents: Int = GlRenderConstants.COLOR_COMPONENTS,
    val positionOffsetFloats: Int = 0,
    val normalOffsetFloats: Int = positionComponents,
    val colorOffsetFloats: Int = positionComponents + normalComponents
) {
    val floatsPerVertex: Int = positionComponents + normalComponents + colorComponents
    val strideBytes: Int = floatsPerVertex * GlRenderConstants.FLOAT_BYTES
}
