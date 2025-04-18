package com.offmind.runtimeshaders.screens.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import com.offmind.runtimeshaders.screens.editor.dialog.manage_node.AddUINodeItem
import com.offmind.runtimeshaders.screens.editor.mock.connections
import com.offmind.runtimeshaders.screens.editor.mock.nodes
import com.offmind.runtimeshaders.screens.editor.model.*
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

    init {
        _state.update {
            it.copy(nodes2 = nodes, connections2 = connections)
        }

        // Run test to verify NodesToCodeUseCase changes
        com.offmind.runtimeshaders.screens.editor.usecase.TestNodesToCodeUseCase.runTest()

        // Update shader after test
        updateShader()
    }

    fun onNodePositionChange(nodeId: Int, newPosition: Offset) {
        _state.update { state ->
            val updatedNodes2 = state.nodes2.map { node ->
                if (node.id == nodeId) {
                    node.copy(uiData = NodeUiData(position = newPosition))
                } else {
                    node
                }
            }.toMutableStateList()
            state.copy(nodes2 = updatedNodes2)
        }
    }

    fun addNode(addUiNodeItem: AddUINodeItem) {
        _state.update { state ->
            val offsetXInDp = state.cameraState.offset.x / state.cameraState.zoom
            val offsetYInDp = state.cameraState.offset.y / state.cameraState.zoom

            val pos = Offset(
                x = state.cameraState.canvasSize.width * 0.25f - offsetXInDp,
                y = 50f * state.cameraState.zoom - offsetYInDp,
            )

            val newNode2 = Node(
                id = state.nodes.size,
                name = "${addUiNodeItem.title} ${state.nodes.size}",
                type = addUiNodeItem.nodeData.toNodeType(),
                pins = emptyList(),
                uiData = NodeUiData(pos)
            )
            val updateNodes2 = state.nodes2.toMutableList()
            updateNodes2.add(newNode2)

            state.copy(nodes2 = updateNodes2.toMutableStateList())
        }
    }

    fun updateCanvasSize(newSize: Size) {
        _state.update {
            it.copy(
                cameraState = it.cameraState.copy(
                    canvasSize = newSize
                )
            )
        }
    }

    fun deleteNode(nodeId: Int) {
        _state.update { state ->
            val nodes = state.nodes.toMutableList()
            val nodes2 = state.nodes2.toMutableList()
            val connections = state.connections.toMutableList()
            val updatedNodes = nodes.filter { it.id != nodeId }
            val updatedNodes2 = nodes2.filter { it.id != nodeId }
            val updatedConnections =
                connections.filter { it.fromNode != nodeId || it.toNode != nodeId }
            state.copy(
                nodes = updatedNodes.toMutableStateList(),
                connections = updatedConnections.toMutableStateList(),
                nodes2 = updatedNodes2.toMutableStateList(),
            )
        }
    }

    fun onCanvasCameraStateChanged(cameraState: CameraState) {
        val oldState = _state.value.cameraState
        _state.update { state ->
            state.copy(
                cameraState = oldState.copy(
                    offset = cameraState.offset,
                    zoom = cameraState.zoom
                )
            )
        }
    }

    fun updateShader() {
        _state.update { state ->
            state.copy(
                shaderCode = nodesToCodeUseCase.invoke(state.nodes2, state.connections2)
            )
        }
    }

    fun onPinUpdated(pin: Pin) {
        _state.update { state ->
            val updatedNodes2 = state.nodes2.map { node ->
                if (node.id == pin.parentId) {
                    val updatedPins = node.pins.map { existingPin ->
                        if (existingPin.id == pin.id) {
                            pin
                        } else {
                            existingPin
                        }
                    }
                    node.copy(pins = updatedPins)
                } else {
                    node
                }
            }.toMutableStateList()
            state.copy(nodes2 = updatedNodes2)
        }
        updateShader()
    }

}

data class CameraState(
    val offset: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val canvasSize: Size = Size.Zero,
)

data class NodeEditorState(
    val nodes: SnapshotStateList<NodeData> = mutableStateListOf(),
    val connections: SnapshotStateList<NodeConnection> = mutableStateListOf(),
    val cameraState: CameraState = CameraState(),
    val connections2: List<Connection> = emptyList(),
    val nodes2: SnapshotStateList<Node> = mutableStateListOf(),
    val shaderCode: String = "",
)
