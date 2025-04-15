package com.offmind.runtimeshaders.screens.editor.dialog.manage_node

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.offmind.runtimeshaders.screens.editor.model.AGVector3
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType

sealed class AddUINodeItem {
    abstract val title: String
    abstract val nodeData: NodeDataType

    data object ColorNode : AddUINodeItem() {
        override val title: String
            get() = "Color Node"
        override val nodeData: NodeDataType
            get() = NodeDataType.ColorNode(Color.White)
    }

    data object UVNode : AddUINodeItem() {
        override val title: String
            get() = "UV Node"
        override val nodeData: NodeDataType
            get() = NodeDataType.UVNode(Offset(0f, 0f))
    }

    data object LengthNode : AddUINodeItem() {
        override val title: String
            get() = "Length Node"
        override val nodeData: NodeDataType
            get() = NodeDataType.LengthNode(0f)
    }

    data object InputNode : AddUINodeItem() {
        override val title: String
            get() = "Input Node"
        override val nodeData: NodeDataType
            get() = NodeDataType.InputNode(AGVector3(0f, 0f, 0f))
    }

    data object OutputNode : AddUINodeItem() {
        override val title: String
            get() = "Output Node"
        override val nodeData: NodeDataType
            get() = NodeDataType.OutputNode(Color.White)
    }

    companion object {
        val items = listOf(
            ColorNode,
            UVNode,
            LengthNode,
            InputNode,
            OutputNode
        )
    }
}
