package com.offmind.runtimeshaders.composables.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.offmind.runtimeshaders.screens.editor.NodeEditorViewModel
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeConnection
import com.offmind.runtimeshaders.screens.editor.model.NodeData
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType
import com.offmind.runtimeshaders.screens.editor.model.Pin
import com.offmind.runtimeshaders.screens.editor.model.PinType

@Composable
fun NodeCanvas(
    modifier: Modifier,
    nodes: List<Node> = emptyList(),
    connections: List<NodeConnection> = emptyList(),
    vm: NodeEditorViewModel? = null,
    onNodePositionChange: (Int, Offset) -> Unit = { _, _ -> },
    onDoubleTap: (Offset) -> Unit = {}
) {
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    // Mutable maps for anchor positions:
    val outputAnchorPositions = remember { mutableStateMapOf<Int, Offset>() }
    val inputAnchorPositions = remember { mutableStateMapOf<Int, Offset>() }

    CanvasWrapper(
        modifier = modifier,
        onDoubleTap = onDoubleTap,
        offsetUpdated = { canvasOffset += it },
    ) {
        /* DrawConnectionLines(
             connections = connections,
             nodes = nodes,
             outputAnchorPositions = outputAnchorPositions,
             inputAnchorPositions = inputAnchorPositions,
             canvasOffset = canvasOffset,
         )*/

        RenderNodes(
            nodes = nodes,
            canvasOffset = canvasOffset,
            vm = vm,
            inputAnchorPositions = inputAnchorPositions,
            outputAnchorPositions = outputAnchorPositions,
            onNodePositionChange = onNodePositionChange,
        )
    }
}

@Composable
private fun CanvasWrapper(
    modifier: Modifier,
    onDoubleTap: (Offset) -> Unit = {},
    offsetUpdated: (Offset) -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = Color.DarkGray)
            .scale(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        onDoubleTap(offset)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    offsetUpdated(dragAmount)
                    change.consume()
                }
            }
            .onGloballyPositioned {
                val size = it.size.toSize()
            }
    ) {
        content()
    }
}

@Composable
fun RenderNodes(
    nodes: List<Node>,
    canvasOffset: Offset,
    vm: NodeEditorViewModel?,
    inputAnchorPositions: MutableMap<Int, Offset>,
    outputAnchorPositions: MutableMap<Int, Offset>,
    onNodePositionChange: (Int, Offset) -> Unit
) {
    for (node in nodes) {
        NodeItem(
            nodeId = node.id,
            nodePosition = node.position,
            canvasOffset = canvasOffset,
            name = node.name,
            pins = node.pins,
            vm = vm,
            onInputAnchorCaptured = { anchor ->
                inputAnchorPositions[node.id] = anchor
            },
            onOutputAnchorCaptured = { anchor ->
                outputAnchorPositions[node.id] = anchor
            }
        ) { id, newPosition ->
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
    onInputAnchorCaptured: (Offset) -> Unit = {},
    onOutputAnchorCaptured: (Offset) -> Unit = {},
    onPositionChange: (Int, Offset) -> Unit,
) {
    var localOffset by remember { mutableStateOf(nodePosition) }
    val width = 220.dp

    Row(
        modifier = Modifier
            .offset {
                IntOffset(
                    (localOffset.x + canvasOffset.x).toInt(),
                    (localOffset.y + canvasOffset.y).toInt()
                )
            }
            .width(width)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    localOffset += dragAmount
                    onPositionChange(nodeId, localOffset)
                    change.consume()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /* if (nodeDataType.canInput) {
             ConnectionPoint(
                 onPositionCaptured = onInputAnchorCaptured
             )
         } else {
             Spacer(modifier = Modifier.size(16.dp))
         }*/
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
                NodePinItem(pin = pin)
            }
        }
        /*if (nodeDataType.canOutput) {
            ConnectionPoint(
                onPositionCaptured = onOutputAnchorCaptured
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }*/
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodePinItem(pin: Pin) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (pin.canInput) {
            ConnectionPoint(
                color = Color.Blue,
                onPositionCaptured = { /* Handle input anchor captured */ }
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }
        when (val data = pin.type) {
            is PinType.FloatRangeType -> {
                Column(modifier = Modifier.weight(1f)) {
                    var pinValue by remember { mutableFloatStateOf(0f) }
                    Text(text = "${pin.name}: ${"%.2f".format(pinValue)}", color = Color.White)
                    Slider(
                        value = pinValue,
                        onValueChange = { pinValue = it },
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
                onPositionCaptured = { /* Handle input anchor captured */ }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeColorItem(
    modifier: Modifier = Modifier,
    initialColor: Color = Color.White,
    onColorChange: (Color) -> Unit = {}  // Callback so you can update your node data model if needed
) {
    // Create mutable state variables for each color component.
    // These values are stored between recompositions.
    var red by remember { mutableStateOf(initialColor.red) }
    var green by remember { mutableStateOf(initialColor.green) }
    var blue by remember { mutableStateOf(initialColor.blue) }
    var alpha by remember { mutableStateOf(initialColor.alpha) }

    // Whenever any color component changes, notify the parent.
    LaunchedEffect(red, green, blue, alpha) {
        onColorChange(Color(red, green, blue, alpha))
    }

    Column(
        modifier = modifier.padding(8.dp)
    ) {
        // A simple preview box showing the current color.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(1.dp)
                .background(Color(red, green, blue, alpha), shape = RoundedCornerShape(8.dp))
                .padding(top = 8.dp)
        )

        // Slider for the red component
        Text(text = "Red: ${"%.2f".format(red)}", color = Color.White)
        Slider(
            value = red,
            onValueChange = { red = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White, shape = CircleShape)
                ) {}
            }
        )

        // Slider for the green component
        Text(text = "Green: ${"%.2f".format(green)}", color = Color.White)
        Slider(
            value = green,
            onValueChange = { green = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White, shape = CircleShape)
                ) {}
            }
        )

        // Slider for the blue component
        Text(text = "Blue: ${"%.2f".format(blue)}", color = Color.White)
        Slider(
            value = blue,
            onValueChange = { blue = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White, shape = CircleShape)
                ) {}
            }
        )

        // Slider for the alpha component
        Text(text = "Alpha: ${"%.2f".format(alpha)}", color = Color.White)
        Slider(
            value = alpha,
            onValueChange = { alpha = it },
            valueRange = 0f..1f,
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

fun getColorByNodeType(nodeDataType: NodeDataType): Color {
    return when (nodeDataType) {
        is NodeDataType.UVNode -> Color.Red
        is NodeDataType.LengthNode -> Color.Blue
        is NodeDataType.OutputNode -> Color.Gray
        is NodeDataType.ColorNode -> Color(0xFF565FA1)
        is NodeDataType.InputNode -> Color.Gray
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
                // Use the parent-relative coordinate instead of the root.
                val size = coordinates.size.toSize()
                val anchor =
                    coordinates.positionInParent() + Offset(size.width / 2, size.height / 2)
                onPositionCaptured(anchor)
            }
    )
}

