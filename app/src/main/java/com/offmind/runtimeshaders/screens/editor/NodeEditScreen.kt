package com.offmind.runtimeshaders.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.composables.ui.NodeCanvas
import com.offmind.runtimeshaders.screens.editor.dialog.DialogState
import com.offmind.runtimeshaders.screens.editor.dialog.manage_node.ManageNodeBottomShitDialog
import com.offmind.runtimeshaders.screens.editor.model.NodeDataType
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.toVec4Type
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NodeEditScreen(paddingValues: PaddingValues) {

    val vm = koinViewModel<NodeEditorViewModel>()
    val state = vm.state.collectAsState()
    var dialogState by remember { mutableStateOf(DialogState(false))}

    val colorNode = state.value.nodes.firstOrNull { it.nodeDataType is NodeDataType.ColorNode }

    val color = (colorNode?.nodeDataType as NodeDataType.ColorNode).color

    Column {
        ShaderWindow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            color = color
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
        ) {
            NodeCanvas(
                modifier = Modifier.fillMaxSize(),
                nodes = state.value.nodes,
                vm = vm,
                connections = state.value.connections,
                onNodePositionChange = { id, position ->
                    vm.onNodePositionChange(id, position)
                },
                onDoubleTap = {
                    dialogState = DialogState(true)
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }
    }

    if (dialogState.showManageNodeDialog) {
        ManageNodeBottomShitDialog(
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = {
                dialogState = DialogState()
            },
            onAddNodeClicked = {
                vm.addNode(it)
                dialogState = DialogState()
            }
        )
    }

}

@Composable
fun ShaderWindow(
    modifier: Modifier,
    color: Color
) {
    val shader = remember {
        Shader(circle).getRuntimeShader()
    }

    println("color: $color")
    Box(
        modifier = modifier
            .padding(PaddingValues(0.dp))
            .background(Color(0xFF1D1D1D)),
        contentAlignment = Alignment.Center
    ) {
        ShadedBox(
            shader = shader,
            shaderUniforms = mapOf(
                "inputColor" to color.toVec4Type()
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(color = Color.White)
            )
        }
    }
}

val circle = """
    
   uniform vec4 inputColor;
    
   vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        
        float r = length(uv);      
        return inputColor*inputColor.a;
   }
""".trimIndent()