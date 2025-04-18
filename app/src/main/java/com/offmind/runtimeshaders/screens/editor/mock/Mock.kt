package com.offmind.runtimeshaders.screens.editor.mock

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeType
import com.offmind.runtimeshaders.screens.editor.model.NodeUiData
import com.offmind.runtimeshaders.screens.editor.model.Pin
import com.offmind.runtimeshaders.screens.editor.model.PinType
import com.offmind.runtimeshaders.screens.editor.model.PinValue

val nodes = mutableStateListOf(
    Node(
        id = 0,
        name = "Color",
        type = NodeType.COLOR,
        pins = listOf(
            Pin(
                id = 0,
                parentId = 0,
                type = PinType.FloatRangeType(min = 0f, max = 1f),
                canOutput = true,
                canInput = true,
                name = "R",
                value = PinValue.FloatRangeValue(0.8f)
            ),
            Pin(
                id = 1,
                parentId = 0,
                type = PinType.FloatRangeType(min = 0f, max = 1f),
                canOutput = true,
                canInput = true,
                name = "G",
                value = PinValue.FloatRangeValue(0.5f)
            ),
            Pin(
                id = 2,
                parentId = 0,
                type = PinType.FloatRangeType(min = 0f, max = 1f),
                canOutput = true,
                canInput = true,
                value = PinValue.FloatRangeValue(0.5f),
                name = "B"
            ),
            Pin(
                id = 3,
                parentId = 0,
                type = PinType.FloatRangeType(min = 0f, max = 1f),
                canOutput = true,
                canInput = true,
                value = PinValue.FloatRangeValue(0.5f),
                name = "A"
            ),
        ),
        uiData = NodeUiData(Offset(10f, 50f))
    ),
    Node(
        id = 1,
        name = "Vec4",
        type = NodeType.VEC4,
        pins = listOf(
            Pin(
                id = 0,
                parentId = 1,
                type = PinType.FloatType,
                canOutput = true,
                canInput = true,
                name = "X",
                value = PinValue.FloatValue(0.3f)
            ),
            Pin(
                id = 1,
                parentId = 1,
                type = PinType.FloatType,
                canOutput = true,
                canInput = true,
                name = "Y"
            ),
            Pin(
                id = 2,
                parentId = 1,
                type = PinType.FloatType,
                canOutput = true,
                canInput = true,
                name = "Z"
            ),
            Pin(
                id = 3,
                parentId = 1,
                type = PinType.FloatType,
                canOutput = true,
                canInput = true,
                name = "W"
            ),
            Pin(
                id = 4,
                parentId = 1,
                type = PinType.FloatType,
                canOutput = true,
                canInput = false,
                name = "Vec4"
            )
        ),
        uiData = NodeUiData(Offset(10f, 50f))
    ),
    Node(
        id = 2,
        name = "Output",
        type = NodeType.OUTPUT,
        pins = listOf(
            Pin(
                id = 0,
                parentId = 2,
                type = PinType.Vec4Type,
                canOutput = false,
                canInput = true,
                name = "Output"
            )
        ),
        uiData = NodeUiData(Offset(10f, 50f)),
    ),
)

val connections = mutableStateListOf(
    Connection(
        fromPin = Connection.PinConnectionItem(
            parentId = 0,
            pinId = 0,
        ),
        toPin = Connection.PinConnectionItem(
            parentId = 1,
            pinId = 0,
        ),
    ),
    Connection(
        fromPin = Connection.PinConnectionItem(
            parentId = 1,
            pinId = 4,
        ),
        toPin = Connection.PinConnectionItem(
            parentId = 2,
            pinId = 0,
        ),
    ),
    Connection(
        fromPin = Connection.PinConnectionItem(
            parentId = 0,
            pinId = 1,
        ),
        toPin = Connection.PinConnectionItem(
            parentId = 1,
            pinId = 1,
        ),
    )
)
