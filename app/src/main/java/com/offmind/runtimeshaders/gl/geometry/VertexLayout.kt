package com.offmind.runtimeshaders.gl.geometry

import com.offmind.runtimeshaders.gl.GlRenderConstants

data class VertexLayout(
    val positionComponents: Int = GlRenderConstants.POSITION_COMPONENTS,
    val colorComponents: Int = GlRenderConstants.COLOR_COMPONENTS,
    val positionOffsetFloats: Int = 0,
    val colorOffsetFloats: Int = positionComponents
) {
    val floatsPerVertex: Int = positionComponents + colorComponents
    val strideBytes: Int = floatsPerVertex * GlRenderConstants.FLOAT_BYTES
}
