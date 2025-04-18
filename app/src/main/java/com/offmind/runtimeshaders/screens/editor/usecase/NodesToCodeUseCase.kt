package com.offmind.runtimeshaders.screens.editor.usecase

import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeType
import com.offmind.runtimeshaders.screens.editor.model.PinValue

class NodesToCodeUseCase {

    fun invoke(nodes: List<Node>, connections: List<Connection>): String {
        val shaderCode = StringBuilder()
        shaderCode.append("vec4 main(float2 fragCoord) {\n")


        // 1. Find the Output node
        val outputNode = nodes.find { it.type == NodeType.OUTPUT }
            ?: throw IllegalArgumentException("No Output node found in the graph")

        // 2. Build a map of upstream connections for each node
        val upstreamConnections = buildUpstreamConnectionsMap(nodes, connections)

        // Set to track emitted nodes to avoid duplicates
        val emittedNodes = mutableSetOf<Int>()

        // Set to track nodes in the current traversal path to detect cycles
        val currentPath = mutableSetOf<Int>()

        // 3. Traverse the graph starting from the Output node
        traverseAndEmitCode(outputNode, upstreamConnections, emittedNodes, currentPath, shaderCode, nodes)

        // 4. Add the final return statement
        val outputVarName = "node_${outputNode.id}"
        shaderCode.append("    return $outputVarName;\n")

        shaderCode.append("}\n")
        return shaderCode.toString()
    }

    private fun buildUpstreamConnectionsMap(
        nodes: List<Node>, 
        connections: List<Connection>
    ): Map<Int, List<Connection>> {
        // Group connections by destination node
        return connections.groupBy { it.toPin.parentId }
    }

    private fun traverseAndEmitCode(
        node: Node,
        upstreamConnections: Map<Int, List<Connection>>,
        emittedNodes: MutableSet<Int>,
        currentPath: MutableSet<Int>,
        shaderCode: StringBuilder,
        allNodes: List<Node>
    ) {
        // Check if node is already in the current path (cycle detection)
        if (currentPath.contains(node.id)) {
            throw IllegalStateException("Cycle detected in the shader graph at node ${node.id}")
        }

        // If node already emitted, no need to process again
        if (emittedNodes.contains(node.id)) {
            return
        }

        // Add node to current path for cycle detection
        currentPath.add(node.id)

        // Get upstream connections for this node
        val nodeConnections = upstreamConnections[node.id] ?: emptyList()

        // Process all inputs first
        for (connection in nodeConnections) {
            val fromNodeId = connection.fromPin.parentId
            val fromNode = allNodes.find { it.id == fromNodeId }
                ?: throw IllegalStateException("Node ${fromNodeId} not found")

            traverseAndEmitCode(fromNode, upstreamConnections, emittedNodes, currentPath, shaderCode, allNodes)
        }

        // Generate code for this node
        generateNodeCode(node, nodeConnections, shaderCode, allNodes)

        // Mark node as emitted
        emittedNodes.add(node.id)

        // Remove node from current path
        currentPath.remove(node.id)
    }

    private fun generateNodeCode(
        node: Node,
        connections: List<Connection>,
        shaderCode: StringBuilder,
        allNodes: List<Node>
    ) {
        val nodeVarName = "node_${node.id}"

        when (node.type) {
            NodeType.COLOR -> {
                // Extract r, g, b, a values from pins
                val r = getValueForPin(node, "R", connections, allNodes, "0.0")
                val g = getValueForPin(node, "G", connections, allNodes, "0.0")
                val b = getValueForPin(node, "B", connections, allNodes, "0.0")
                val a = getValueForPin(node, "A", connections, allNodes, "1.0")

                shaderCode.append("    vec4 $nodeVarName = vec4($r, $g, $b, $a);\n")
            }
            NodeType.MIX -> {
                // Extract in1, in2, proportion values
                val in1 = getValueForPin(node, "In1", connections, allNodes, "vec4(0.0)")
                val in2 = getValueForPin(node, "In2", connections, allNodes, "vec4(0.0)")
                val proportion = getValueForPin(node, "Proportion", connections, allNodes, "0.5")

                shaderCode.append("    vec4 $nodeVarName = mix($in1, $in2, $proportion);\n")
            }
            NodeType.FLOAT -> {
                // Extract value
                val value = getValueForPin(node, "Value", connections, allNodes, "0.0")

                shaderCode.append("    float $nodeVarName = $value;\n")
            }
            NodeType.VEC4 -> {
                // Extract x, y, z, w values
                val x = getValueForPin(node, "X", connections, allNodes, "0.0")
                val y = getValueForPin(node, "Y", connections, allNodes, "0.0")
                val z = getValueForPin(node, "Z", connections, allNodes, "0.0")
                val w = getValueForPin(node, "W", connections, allNodes, "1.0")

                shaderCode.append("    vec4 $nodeVarName = vec4($x, $y, $z, $w);\n")
            }
            NodeType.OUTPUT -> {
                // For output node, we'll just reference its input
                val input = getValueForPin(node, "Output", connections, allNodes, "vec4(0.0, 0.0, 0.0, 1.0)")

                shaderCode.append("    vec4 $nodeVarName = $input;\n")
            }
        }
    }

    private fun getValueForPin(
        node: Node,
        pinName: String,
        connections: List<Connection>,
        allNodes: List<Node>,
        defaultValue: String
    ): String {
        // Find the pin by name
        val pin = node.pins.find { it.name == pinName }
            ?: return defaultValue

        // Check if there's a connection to this pin
        val connection = connections.find { it.toPin.parentId == node.id && it.toPin.pinId == pin.id }

        if (connection != null) {
            // Get the source node and pin
            val fromNodeId = connection.fromPin.parentId
            val fromPinId = connection.fromPin.pinId

            val fromNode = allNodes.find { it.id == fromNodeId }
                ?: throw IllegalStateException("Source node ${fromNodeId} not found")

            val fromPin = fromNode.pins.find { it.id == fromPinId }
                ?: throw IllegalStateException("Source pin ${fromPinId} not found in node ${fromNodeId}")

            // Check if we need to add a component suffix (e.g., .x) for type conversion
            val sourceVarName = "node_${fromNodeId}"

            // If the destination expects a float and the source is a vec4, use the .x component
            val isDestFloatType = node.type == NodeType.FLOAT || 
                                 pinName in listOf("R", "G", "B", "A", "X", "Y", "Z", "W", "Value", "Proportion")
            val isSourceVec4Type = fromNode.type == NodeType.VEC4 || 
                                  fromNode.type == NodeType.COLOR || 
                                  fromNode.type == NodeType.MIX || 
                                  fromNode.type == NodeType.OUTPUT

            if (isDestFloatType && isSourceVec4Type) {
                // Get the component based on the pin name
                val component = when (fromPin.name.lowercase()) {
                    "r", "x" -> "r"
                    "g", "y" -> "g"
                    "b", "z" -> "b"
                    "a", "w" -> "a"
                    else -> "x" // Default to x if no matching component
                }
                return "$sourceVarName.$component"
            }

            // Return the variable name for the source node
            return sourceVarName
        }

        // If no connection, check if the pin has a value
        pin.value?.let { value ->
            return when (value) {
                is PinValue.FloatValue -> value.value.toString()
                is PinValue.Vec2Value -> "vec2(${value.x}, ${value.y})"
                is PinValue.Vec3Value -> "vec3(${value.x}, ${value.y}, ${value.z})"
                is PinValue.Vec4Value -> "vec4(${value.x}, ${value.y}, ${value.z}, ${value.w})"
                is PinValue.IntValue -> value.value.toString()
                is PinValue.BoolValue -> if (value.value) "1.0" else "0.0"
                is PinValue.StringValue -> "\"${value.value}\""
                is PinValue.FloatRangeValue -> value.value.toString()
            }
        }

        // If no connection and no value, return default value
        return defaultValue
    }
}
