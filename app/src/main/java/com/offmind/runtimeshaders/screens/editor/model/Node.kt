package com.offmind.runtimeshaders.screens.editor.model

import androidx.compose.ui.geometry.Offset

data class Node(
    val id: Int,
    val name: String,
    val type: NodeType,
    val position: Offset,
    val pins: List<Pin>,
    val uiData: NodeUiData,
)

data class NodeUiData(
    val position: Offset,
)

data class Pin(
    val id: Int,
    val parentId: Int,
    val type: PinType,
    val canOutput: Boolean,
    val canInput: Boolean,
    val name: String,
)

data class Connection(
    val connection: Map<String, String>
)

sealed class PinType {
    data object FloatType : PinType()
    data object Vec2Type : PinType()
    data object Vec3Type : PinType()
    data object Vec4Type : PinType()
    data object IntType : PinType()
    data object BoolType : PinType()
    data object StringType : PinType()
    data class FloatRangeType(val min: Float, val max: Float) : PinType()
}

enum class NodeType {
    COLOR,
    MIX,
    FLOAT,
    VEC4,
    OUTPUT,
}