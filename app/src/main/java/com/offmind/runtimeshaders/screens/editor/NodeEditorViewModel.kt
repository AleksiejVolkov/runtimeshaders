package com.offmind.runtimeshaders.screens.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.offmind.runtimeshaders.screens.editor.dialog.manage_node.AddUINodeItem
import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeConnection
import com.offmind.runtimeshaders.screens.editor.model.NodeData
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType
import com.offmind.runtimeshaders.screens.editor.model.NodeType
import com.offmind.runtimeshaders.screens.editor.model.NodeUiData
import com.offmind.runtimeshaders.screens.editor.model.Pin
import com.offmind.runtimeshaders.screens.editor.model.PinType
import com.offmind.runtimeshaders.screens.editor.usecase.NodesToCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class NodeEditorViewModel(
    val nodesToCodeUseCase: NodesToCodeUseCase,
) : ViewModel() {

    private val _state: MutableStateFlow<NodeEditorState> = MutableStateFlow(NodeEditorState())
    val state: StateFlow<NodeEditorState> = _state

    companion object {
        const val OUTPUT_NODE_ID = -1
    }

    private val nodes = mutableStateListOf(
        Node(
            id = 0,
            name = "Color",
            type = NodeType.COLOR,
            position = Offset(10f, 50f),
            pins = listOf(
                Pin(
                    id = 0,
                    parentId = 0,
                    type = PinType.FloatRangeType(min = 0f, max = 1f),
                    canOutput = true,
                    canInput = true,
                    name = "R"
                ),
                Pin(
                    id = 1,
                    parentId = 0,
                    type = PinType.FloatRangeType(min = 0f, max = 1f),
                    canOutput = true,
                    canInput = true,
                    name = "G"
                ),
                Pin(
                    id = 2,
                    parentId = 0,
                    type = PinType.FloatRangeType(min = 0f, max = 1f),
                    canOutput = true,
                    canInput = true,
                    name = "B"
                ),
                Pin(
                    id = 3,
                    parentId = 0,
                    type = PinType.FloatRangeType(min = 0f, max = 1f),
                    canOutput = true,
                    canInput = true,
                    name = "A"
                ),
            ),
            uiData = NodeUiData(Offset(10f, 50f))
        ),
        Node(
            id = 1,
            name = "Vec4",
            type = NodeType.VEC4,
            position = Offset(10f, 50f),
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
            position = Offset(10f, 50f),
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

    init {
        _state.update {
            it.copy(nodes2 = nodes)
        }
    }

    fun onNodePositionChange(nodeId: Int, newPosition: Offset) {
        _state.update { state ->
            val updatedNodes = state.nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(position = newPosition)
                } else {
                    node
                }
            }.toMutableStateList()
            state.copy(nodes = updatedNodes)
        }
    }

    fun onNodeColorChange(nodeId: Int, newColor: Color) {
        _state.update { state ->
            val updatedNodes = state.nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        nodeDataType = (node.nodeDataType as NodeDataType.ColorNode).copy(
                            color = newColor
                        )
                    )
                } else {
                    node
                }
            }.toMutableStateList()
            state.copy(nodes = updatedNodes)
        }
    }

    fun addNode(addUiNodeItem: AddUINodeItem) {
        _state.update { state ->
            val newNode = NodeData(
                id = state.nodes.size,
                name = addUiNodeItem.title,
                position = Offset(10f, 50f), //todo decide position of new node
                nodeDataType = addUiNodeItem.nodeData
            )
            val updatedNodes = state.nodes.toMutableList()
            updatedNodes.add(newNode)
            state.copy(nodes = updatedNodes.toMutableStateList())
        }
    }
}

data class NodeEditorState(
    val nodes: SnapshotStateList<NodeData> = mutableStateListOf(),
    val nodes2: SnapshotStateList<Node> = mutableStateListOf(),
    val connections: List<NodeConnection> = emptyList(),
    val connections2: List<Connection> = emptyList(),
)