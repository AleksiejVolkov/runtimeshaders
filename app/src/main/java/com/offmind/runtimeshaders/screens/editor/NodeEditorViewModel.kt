package com.offmind.runtimeshaders.screens.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.offmind.runtimeshaders.screens.editor.dialog.manage_node.AddUINodeItem
import com.offmind.runtimeshaders.screens.editor.model.NodeConnection
import com.offmind.runtimeshaders.screens.editor.model.NodeData
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType
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

    private val startNodes = mutableStateListOf(
        NodeData(
            id = 0,
            name = "Color",
            position = Offset(10f, 50f),
            nodeDataType = NodeDataType.ColorNode(Color.White)
        ),
        NodeData(
            id = OUTPUT_NODE_ID,
            name = "Output",
            position = Offset(300f, 50f),
            nodeDataType = NodeDataType.OutputNode(Color.Black)
        )
    )

    private val connections: List<NodeConnection> = listOf(
        NodeConnection(
            fromNode = 0,
            toNode = OUTPUT_NODE_ID,
        )
    )

    init {
        _state.update {
            it.copy(
                nodes = startNodes,
                connections = connections,
            )
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
                    node.copy(nodeDataType = (node.nodeDataType as NodeDataType.ColorNode).copy(color = newColor))
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
    val connections: List<NodeConnection> = emptyList(),
)