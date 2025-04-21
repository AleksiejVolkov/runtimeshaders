package com.offmind.runtimeshaders.composables.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeConnection
import com.offmind.runtimeshaders.screens.editor.model.NodeData

@Composable
fun DrawPinConnectionLines(
    connections: List<Connection>,
    nodes: List<Node>,
    inputPinPositions: Map<Connection.PinConnectionItem, Offset>,
    outputPinPositions: Map<Connection.PinConnectionItem, Offset>,
    canvasOffset: Offset
) {
    val controlOffset = with(LocalDensity.current) { 10.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        connections.forEach { connection ->
            val fromPinPosition = outputPinPositions[connection.fromPin]
            val toPinPosition = inputPinPositions[connection.toPin]

            if (fromPinPosition != null && toPinPosition != null) {
                val startPoint = fromPinPosition - nodes.find { it.id == connection.fromPin.parentId }!!.uiData.position
                val endPoint = toPinPosition - nodes.find { it.id == connection.toPin.parentId }!!.uiData.position

                val controlPoint1 = Offset(startPoint.x + controlOffset, startPoint.y)
                val controlPoint2 = Offset(endPoint.x - controlOffset, endPoint.y)

                val path = Path().apply {
                    moveTo(startPoint.x, startPoint.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        endPoint.x, endPoint.y
                    )
                }

                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
