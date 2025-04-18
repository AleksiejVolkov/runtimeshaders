package com.offmind.runtimeshaders.screens.editor.dialog.manage_node

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.screens.editor.model.Node
import com.offmind.runtimeshaders.screens.editor.model.NodeData
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType
import com.offmind.runtimeshaders.screens.editor.model.NodeType
import com.offmind.runtimeshaders.screens.editor.model.NodeUiData

/**
 * Created by Vladyslav Abramitov on 16.04.2025.
 */
@Composable
fun DeleteNodePopUp(
    nodeData: Node,
    onDismiss: () -> Unit = {},
    onDeleteClicked: (Int) -> Unit = {},
) {
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = -32, y = 32),
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier.widthIn(max = 240.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.do_you_want_delete_node, nodeData.name),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    FilledTonalButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            stringResource(R.string.common_cancel),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onDeleteClicked(nodeData.id)
                        }
                    ) {
                        Text(
                            stringResource(R.string.common_delete),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
fun DeleteNodePopUpPreview() {
    Box(Modifier
        .fillMaxSize()
        .background(Color.DarkGray)) {
        DeleteNodePopUp(
            nodeData = Node(
                id = 0,
                name = "SomeName",
                pins = emptyList(),
                uiData = NodeUiData(Offset.Zero),
                type = NodeType.COLOR,
            )
        )
    }
}