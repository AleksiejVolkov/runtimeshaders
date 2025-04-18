package com.offmind.runtimeshaders.screens.editor.model

import androidx.compose.ui.geometry.Offset

data class Node(
    val id: Int,
    val name: String,
    val type: NodeType,
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
    val value: PinValue? = null,
)

sealed class PinValue {
    data class FloatValue(val value: Float) : PinValue()
    data class Vec2Value(val x: Float, val y: Float) : PinValue()
    data class Vec3Value(val x: Float, val y: Float, val z: Float) : PinValue()
    data class Vec4Value(val x: Float, val y: Float, val z: Float, val w: Float) : PinValue()
    data class IntValue(val value: Int) : PinValue()
    data class BoolValue(val value: Boolean) : PinValue()
    data class StringValue(val value: String) : PinValue()
    data class FloatRangeValue(val value: Float) : PinValue()
}

data class Connection(
    val fromPin: PinConnectionItem,
    val toPin: PinConnectionItem
) {
    data class PinConnectionItem(val parentId: Int, val pinId: Int)
}

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
