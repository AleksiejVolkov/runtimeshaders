package com.offmind.runtimeshaders.composables.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.offmind.runtimeshaders.screens.editor.CameraState
import com.offmind.runtimeshaders.screens.editor.NodeEditorViewModel
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.model.Pin
import com.offmind.runtimeshaders.screens.editor.model.PinType
import com.offmind.runtimeshaders.screens.editor.model.PinValue

@Composable
fun NodeCanvas(
    modifier: Modifier,
    nodes: List<Node> = emptyList(),
    connections: List<Connection> = emptyList(),
    cameraState: CameraState,
    vm: NodeEditorViewModel? = null,
    onNodePositionChange: (Int, Offset) -> Unit = { _, _ -> },
    onCameraStateChange: (CameraState) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onLongPress: (Offset, Node) -> Unit = { _, _ -> }
) {
    var canvasOffset by remember { mutableStateOf(cameraState.offset) }

    // Mutable map for pin positions:
    val outputPinPositions = remember { mutableStateMapOf<Connection.PinConnectionItem, Offset>() }
    val inputPinPositions = remember { mutableStateMapOf<Connection.PinConnectionItem, Offset>() }

    CanvasWrapper(
        modifier = modifier
            .onGloballyPositioned {
                vm?.updateCanvasSize(it.size.toSize())
            },
        onDoubleTap = onDoubleTap,
        canvasOffset = canvasOffset,
        cameraState = cameraState,
        offsetUpdated = { offset, scale ->
            canvasOffset += offset
            onCameraStateChange(CameraState(canvasOffset, scale))
        },
    ) {
        RenderNodes(
            nodes = nodes,
            canvasOffset = canvasOffset,
            vm = vm,
            outputPinPositions = outputPinPositions,
            inputPinPositions = inputPinPositions,
            onLongPress = onLongPress,
            onNodePositionChange = onNodePositionChange,
        )

        DrawPinConnectionLines(
            connections = connections,
            inputPinPositions = inputPinPositions,
            outputPinPositions = outputPinPositions,
            nodes = nodes,
            canvasOffset = canvasOffset,
        )
    }
}

@Composable
private fun CanvasWrapper(
    modifier: Modifier,
    canvasOffset: Offset,
    cameraState: CameraState,
    onDoubleTap: (Offset) -> Unit = {},
    offsetUpdated: (Offset, Float) -> Unit = { _, _ -> },
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(cameraState.zoom) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.1f, 10f)
        offsetUpdated(panChange, scale)
    }

    Box(
        modifier = modifier
            .background(color = Color.DarkGray)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = onDoubleTap)
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 10f)
                    offsetUpdated(pan, scale)
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = canvasOffset.x
                translationY = canvasOffset.y
            }
            .transformable(state = transformState)
    ) {
        content()
    }
}

@Composable
fun RenderNodes(
    nodes: List<Node>,
    canvasOffset: Offset,
    vm: NodeEditorViewModel?,
    outputPinPositions: MutableMap<Connection.PinConnectionItem, Offset>,
    inputPinPositions: MutableMap<Connection.PinConnectionItem, Offset>,
    onNodePositionChange: (Int, Offset) -> Unit,
    onLongPress: (Offset, Node) -> Unit = { _, _ -> },
) {
    val haptic = LocalHapticFeedback.current

    for (node in nodes) {
        NodeItem(
            nodeId = node.id,
            nodePosition = node.uiData.position,
            canvasOffset = canvasOffset,
            name = node.name,
            pins = node.pins,
            vm = vm,
            outputPinPositions = outputPinPositions,
            inputPinPositions = inputPinPositions,
            onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress(it, node)
            }) { id, newPosition ->
            onNodePositionChange(id, newPosition)
        }
    }
}

@Composable
fun NodeItem(
    nodeId: Int,
    nodePosition: Offset,
    canvasOffset: Offset,
    name: String,
    pins: List<Pin>,
    vm: NodeEditorViewModel? = null,
    outputPinPositions: MutableMap<Connection.PinConnectionItem, Offset>,
    inputPinPositions: MutableMap<Connection.PinConnectionItem, Offset>,
    onLongPress: (Offset) -> Unit = {},
    onPositionChange: (Int, Offset) -> Unit,
) {
    var localOffset by remember { mutableStateOf(nodePosition) }
    val width = 220.dp

    Row(
        modifier = Modifier
            .offset {
                IntOffset(
                    (localOffset.x).toInt(),
                    (localOffset.y).toInt()
                )
            }
            .width(width)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    localOffset += dragAmount
                    onPositionChange(nodeId, localOffset)
                    change.consume()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress(localOffset)
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .shadow(4.dp)
                .weight(1f)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NodeTitleItem(
                title = name,
                modifier = Modifier.weight(1f)
            )
            pins.forEach { pin ->
                NodePinItem(
                    pin = pin,
                    outputPinPositions = outputPinPositions,
                    inputPinPositions = inputPinPositions,
                ) {
                    vm?.onPinUpdated(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodePinItem(
    pin: Pin,
    outputPinPositions: MutableMap<Connection.PinConnectionItem, Offset>,
    inputPinPositions: MutableMap<Connection.PinConnectionItem, Offset>,
    onPinUpdated: (Pin) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (pin.canInput) {
            ConnectionPoint(
                color = Color.Blue,
                onPositionCaptured = { position ->
                    inputPinPositions[Connection.PinConnectionItem(pin.parentId, pin.id)] = position
                }
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }
        when (val data = pin.type) {
            is PinType.FloatRangeType -> {
                Column(modifier = Modifier.weight(1f)) {
                    if(pin.value != null) {
                        var pinValue by remember { mutableFloatStateOf((pin.value as PinValue.FloatRangeValue).value) }
                        Text(text = "${pin.name}: ${"%.2f".format(pinValue)}", color = Color.White)
                        Slider(
                            value = pinValue,
                            onValueChange = {
                                pinValue = it
                                onPinUpdated(pin.copy(value = PinValue.FloatRangeValue(pinValue)))
                            },
                            valueRange = data.min..data.max,
                            modifier = Modifier.fillMaxWidth(),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.White, shape = CircleShape)
                                ) {}
                            }
                        )
                    }
                }
            }

            is PinType.Vec2Type -> {
                Text("Vec2 Pin")
            }

            is PinType.Vec3Type -> {
                Text("Vec3 Pin")
            }

            is PinType.Vec4Type -> {
                Text("Vec4 Pin")
            }

            is PinType.IntType -> {
                Text("Int Pin")
            }

            is PinType.BoolType -> {
                Text("Bool Pin")
            }

            is PinType.StringType -> {
                Text("String Pin")
            }

            is PinType.FloatType -> {
                Text("Float Pin")
            }
        }
        if (pin.canOutput) {
            ConnectionPoint(
                color = Color.Green,
                onPositionCaptured = { position ->
                    outputPinPositions[Connection.PinConnectionItem(pin.parentId, pin.id)] = position
                }
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun NodeTitleItem(
    modifier: Modifier = Modifier,
    title: String,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = modifier.padding(8.dp),
            color = Color.White
        )
    }
}

@Composable
fun ConnectionPoint(
    modifier: Modifier = Modifier,
    color: Color = Color.Blue,
    onPositionCaptured: (Offset) -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(Color.White, shape = CircleShape)
            .padding(2.dp)
            .background(color = color, shape = CircleShape)
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size.toSize()
                val anchor =
                    coordinates.positionInParent() + Offset(size.width / 2, size.height / 2)
                onPositionCaptured(anchor)
            }
    )
}