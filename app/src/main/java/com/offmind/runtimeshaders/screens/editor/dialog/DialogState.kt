package com.offmind.runtimeshaders.screens.editor.dialog

import com.offmind.runtimeshaders.screens.editor.model.NodeData

data class DialogState(
    val showManageNodeDialog: Boolean = false,
    val showDeleteNodeDialog: NodeData? = null,
)
