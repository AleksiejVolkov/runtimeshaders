package com.offmind.runtimeshaders.composables.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.offmind.runtimeshaders.screens.editor.NodeEditorViewModel
import com.offmind.runtimeshaders.screens.editor.model.NodeConnection
import com.offmind.runtimeshaders.screens.editor.model.NodeData
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType

@Composable
fun NodeCanvas(
    modifier: Modifier,
    nodes: List<NodeData> = emptyList(),
    connections: List<NodeConnection> = emptyList(),
    vm: NodeEditorViewModel? = null,
    onNodePositionChange: (Int, Offset) -> Unit = { _, _ -> },
    onDoubleTap: (Offset) -> Unit = {},
    onLongPress: (Offset, NodeData) -> Unit = { _, _ -> }
) {
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    // Mutable maps for anchor positions:
    val outputAnchorPositions = remember { mutableStateMapOf<Int, Offset>() }
    val inputAnchorPositions = remember { mutableStateMapOf<Int, Offset>() }

    Box(
        modifier = modifier
            .background(color = Color.DarkGray)
            .scale(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = onDoubleTap
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    canvasOffset += dragAmount
                    change.consume()
                }
            }
            .onGloballyPositioned {
                val size = it.size.toSize()

            }
    ) {
        // Draw connection lines first
        Canvas(modifier = Modifier.fillMaxSize()) {
            connections.forEach { connection ->
                // Use the captured anchor positions for output and input.
                // Fallback to using your node position + an offset if not yet captured.
                val outputAnchorPos = outputAnchorPositions[connection.fromNode] ?: return@Canvas
                val fromNodePos = nodes.find { it.id == connection.fromNode }?.position ?: return@Canvas
                val startPoint = outputAnchorPos + fromNodePos + canvasOffset

                val inputAnchorPos = inputAnchorPositions[connection.toNode] ?: return@Canvas
                val toNodePos = nodes.find { it.id == connection.toNode }?.position ?: return@Canvas
                val endPoint = inputAnchorPos + toNodePos + canvasOffset

                // Define a horizontal offset for the control points.
                // You might want to adjust these values based on your UI needs.
                val controlOffset =
                    100f  // This can be dynamic based on start/end distances if needed

                // First control point: a bit to the right of the start point.
                val controlPoint1 = Offset(startPoint.x + controlOffset, startPoint.y)
                // Second control point: a bit to the left of the end point.
                val controlPoint2 = Offset(endPoint.x - controlOffset, endPoint.y)

                // Create the path with a cubic Bézier curve.
                val path = Path().apply {
                    moveTo(startPoint.x, startPoint.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        endPoint.x, endPoint.y
                    )
                }

                // Draw the path on the Canvas.
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        for (node in nodes) {
            NodeItem(
                nodeId = node.id,
                nodePosition = node.position,
                canvasOffset = canvasOffset,
                name = node.name,
                nodeDataType = node.nodeDataType,
                vm = vm,
                onInputAnchorCaptured = { anchor ->
                    inputAnchorPositions[node.id] = anchor
                },
                onOutputAnchorCaptured = { anchor ->
                    outputAnchorPositions[node.id] = anchor
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(it, node)
                }
            ) { id, newPosition ->
                onNodePositionChange(id, newPosition)
            }
        }
    }
}

@Composable
fun NodeItem(
    nodeId: Int,
    nodePosition: Offset,
    canvasOffset: Offset,
    name: String,
    nodeDataType: NodeDataType,
    vm: NodeEditorViewModel? = null,
    onInputAnchorCaptured: (Offset) -> Unit = {},
    onOutputAnchorCaptured: (Offset) -> Unit = {},
    onLongPress: (Offset) -> Unit = {},
    onPositionChange: (Int, Offset) -> Unit,
) {
    var localOffset by remember { mutableStateOf(nodePosition) }
    val width = if (nodeDataType is NodeDataType.ColorNode) 220.dp else 120.dp

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
        if (nodeDataType.canInput) {
            ConnectionPoint(
                onPositionCaptured = onInputAnchorCaptured
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }
        Column(
            modifier = Modifier
                .shadow(4.dp)
                .weight(1f)
                .background(
                    color = getColorByNodeType(nodeDataType),
                    shape = RoundedCornerShape(8.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NodeTitleItem(
                title = name,
                modifier = Modifier.weight(1f)
            )
            if (nodeDataType is NodeDataType.ColorNode) {
                NodeColorItem(
                    initialColor = nodeDataType.color,
                    onColorChange = { color ->
                        vm?.onNodeColorChange(nodeId, color)
                    }
                )
            }
        }
        if (nodeDataType.canOutput) {
            ConnectionPoint(
                onPositionCaptured = onOutputAnchorCaptured
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
                Box(modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, shape = CircleShape)) {}
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
                Box(modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, shape = CircleShape)) {}
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
                Box(modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, shape = CircleShape)) {}
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
                Box(modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, shape = CircleShape)) {}
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
            .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
            .padding(2.dp)
            .background(color = color, shape = androidx.compose.foundation.shape.CircleShape)
            .onGloballyPositioned { coordinates ->
                // Use the parent-relative coordinate instead of the root.
                val size = coordinates.size.toSize()
                val anchor =
                    coordinates.positionInParent() + Offset(size.width / 2, size.height / 2)
                onPositionCaptured(anchor)
            }
    )
}

