package com.offmind.runtimeshaders.screens.editor.usecase

import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeType
import com.offmind.runtimeshaders.screens.editor.model.NodeUiData
import com.offmind.runtimeshaders.screens.editor.model.Pin
import com.offmind.runtimeshaders.screens.editor.model.PinType
import com.offmind.runtimeshaders.screens.editor.model.PinValue
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class NodesToCodeUseCaseTest {

    @Test
    fun `test component selection based on pin name`() {
        // Create a simple graph with a COLOR node connected to a VEC4 node
        val colorNode = Node(
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
        )

        val vec4Node = Node(
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
                    name = "X"
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
                    type = PinType.Vec4Type,
                    canOutput = true,
                    canInput = false,
                    name = "Vec4"
                )
            ),
            uiData = NodeUiData(Offset(10f, 50f))
        )

        val outputNode = Node(
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
            uiData = NodeUiData(Offset(10f, 50f))
        )

        // Connect R to X and G to Y
        val connections = listOf(
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 0,
                    pinId = 0, // R
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 0, // X
                ),
            ),
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 0,
                    pinId = 1, // G
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 1, // Y
                ),
            ),
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 4, // Vec4
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 2,
                    pinId = 0, // Output
                ),
            )
        )

        val nodes = listOf(colorNode, vec4Node, outputNode)

        val useCase = NodesToCodeUseCase()
        val code = useCase.invoke(nodes, connections)

        // The generated code should use .r for X and .g for Y
        val expectedCode = """vec4 main(float2 fragCoord) {
    vec4 node_0 = vec4(0.8, 0.5, 0.5, 0.5);
    vec4 node_1 = vec4(node_0.r, node_0.g, 0.0, 1.0);
    vec4 node_2 = node_1;
    return node_2;
}
"""
        assertEquals(expectedCode, code)
    }

    @Test
    fun `test component selection with swapped mappings`() {
        // Create a graph with a COLOR node connected to a VEC4 node with swapped mappings
        val colorNode = Node(
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
                    value = PinValue.FloatRangeValue(0.3f),
                    name = "B"
                ),
                Pin(
                    id = 3,
                    parentId = 0,
                    type = PinType.FloatRangeType(min = 0f, max = 1f),
                    canOutput = true,
                    canInput = true,
                    value = PinValue.FloatRangeValue(1.0f),
                    name = "A"
                ),
            ),
            uiData = NodeUiData(Offset(10f, 50f))
        )

        val vec4Node = Node(
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
                    name = "X"
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
                    type = PinType.Vec4Type,
                    canOutput = true,
                    canInput = false,
                    name = "Vec4"
                )
            ),
            uiData = NodeUiData(Offset(10f, 50f))
        )

        val outputNode = Node(
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
            uiData = NodeUiData(Offset(10f, 50f))
        )

        // Connect R to Y, G to X, B to Z, A to W (swapped mappings)
        val connections = listOf(
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 0,
                    pinId = 0, // R
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 1, // Y
                ),
            ),
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 0,
                    pinId = 1, // G
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 0, // X
                ),
            ),
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 0,
                    pinId = 2, // B
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 2, // Z
                ),
            ),
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 0,
                    pinId = 3, // A
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 3, // W
                ),
            ),
            Connection(
                fromPin = Connection.PinConnectionItem(
                    parentId = 1,
                    pinId = 4, // Vec4
                ),
                toPin = Connection.PinConnectionItem(
                    parentId = 2,
                    pinId = 0, // Output
                ),
            )
        )

        val nodes = listOf(colorNode, vec4Node, outputNode)

        val useCase = NodesToCodeUseCase()
        val code = useCase.invoke(nodes, connections)

        // The generated code should use .r for Y, .g for X, .b for Z, and .a for W
        val expectedCode = """vec4 main(float2 fragCoord) {
    vec4 node_0 = vec4(0.8, 0.5, 0.3, 1.0);
    vec4 node_1 = vec4(node_0.g, node_0.r, node_0.b, node_0.a);
    vec4 node_2 = node_1;
    return node_2;
}
"""
        assertEquals(expectedCode, code)
    }
}
