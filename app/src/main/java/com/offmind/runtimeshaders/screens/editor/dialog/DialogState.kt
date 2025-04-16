package com.offmind.runtimeshaders.screens.editor.dialog

import com.offmind.runtimeshaders.screens.editor.model.Node

data class DialogState(
    val showManageNodeDialog: Boolean = false,
    val showDeleteNodeDialog: Node? = null,
)
