package com.offmind.runtimeshaders.screens.editor.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class NodeData(
    val id: Int,
    val name: String,
    val position: Offset,
    val nodeDataType: NodeDataType,
)

data class NodeConnection(
    val fromNode: Int,
    val toNode: Int,
)

sealed class NodeDataType(val canInput: Boolean, val canOutput: Boolean) {
    data class InputNode(val image: AGVector3) : NodeDataType(canInput = false, canOutput = true)
    data class UVNode(val translate: Offset) : NodeDataType(canInput = true, canOutput = true)
    data class LengthNode(val length: Float) : NodeDataType(canInput = true, canOutput = true)
    data class OutputNode(val color: Color) : NodeDataType(canInput = true, canOutput = false)
    data class ColorNode(val color: Color) : NodeDataType(canInput = false, canOutput = true)
}

data class AGVector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun toColor(): Color {
        return Color(x, y, z, 1f)
    }
}