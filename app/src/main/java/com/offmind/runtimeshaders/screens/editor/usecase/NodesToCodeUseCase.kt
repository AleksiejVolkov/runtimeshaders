package com.offmind.runtimeshaders.screens.editor.usecase

import com.offmind.runtimeshaders.screens.editor.NodeEditorViewModel
import com.offmind.runtimeshaders.screens.editor.model.NodeConnection
import com.offmind.runtimeshaders.screens.editor.model.NodeData
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType

class NodesToCodeUseCase {

    fun invoke(nodes: List<NodeData>, connections: List<NodeConnection>): String{
        val shaderCode = StringBuilder()
        shaderCode.append("void main() {\n")

        val outputNode = nodes.firstOrNull { it.id == NodeEditorViewModel.OUTPUT_NODE_ID }
        val connection = connections.firstOrNull { it.toNode == NodeEditorViewModel.OUTPUT_NODE_ID }

        for (node in nodes) {
            when (node.nodeDataType) {
                is NodeDataType.ColorNode -> {
                    shaderCode.append("    vec4 ${node.name} = vec4(${node.nodeDataType.color.red}, ${node.nodeDataType.color.green}, ${node.nodeDataType.color.blue}, 1.0);\n")
                }
                is NodeDataType.OutputNode -> {
                    shaderCode.append("    gl_FragColor = ${node.name};\n")
                }
                // Handle other node types here
                is NodeDataType.InputNode -> TODO()
                is NodeDataType.LengthNode -> TODO()
                is NodeDataType.UVNode -> TODO()
            }
        }

        shaderCode.append("}\n")
        return shaderCode.toString()
    }
}