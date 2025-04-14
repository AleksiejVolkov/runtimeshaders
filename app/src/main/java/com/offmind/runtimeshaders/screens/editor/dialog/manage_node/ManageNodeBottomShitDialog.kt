package com.offmind.runtimeshaders.screens.editor.dialog.manage_node

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageNodeBottomShitDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onAddNodeClicked: (AddUINodeItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Text(modifier = Modifier.padding(10.dp),text = stringResource(R.string.add_new_node), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(AddUINodeItem.items) { item ->
                AddNodeItem(nodeUiType = item, onItemClick = onAddNodeClicked)
            }
        }
    }
}

@Composable
fun AddNodeItem(
    modifier: Modifier = Modifier,
    nodeUiType: AddUINodeItem,
    onItemClick: (AddUINodeItem) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onItemClick(nodeUiType) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(nodeUiType.title, style = MaterialTheme.typography.bodyMedium)
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add new node")
    }
}