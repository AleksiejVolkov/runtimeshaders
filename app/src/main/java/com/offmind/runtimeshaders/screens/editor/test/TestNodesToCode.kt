package com.offmind.runtimeshaders.screens.editor.test

import com.offmind.runtimeshaders.screens.editor.mock.connections
import com.offmind.runtimeshaders.screens.editor.mock.nodes
import com.offmind.runtimeshaders.screens.editor.model.Connection
import com.offmind.runtimeshaders.screens.editor.usecase.NodesToCodeUseCase

/**
 * Simple test function to verify the NodesToCodeUseCase implementation.
 */
fun main() {
    println("Testing NodesToCodeUseCase with mock data")
    
    val useCase = NodesToCodeUseCase()
    
    // Test with mock data
    try {
        val shaderCode = useCase.invoke(nodes, connections)
        println("\nGenerated shader code:")
        println("----------------------")
        println(shaderCode)
        println("----------------------")
        println("Test passed: Successfully generated shader code")
    } catch (e: Exception) {
        println("Test failed: ${e.message}")
        e.printStackTrace()
    }
    
    // Test cycle detection
    println("\nTesting cycle detection")
    
    // Create a cycle: node0 -> node1 -> node2 -> node0
    val cycleNodes = nodes.toMutableList()
    val cycleConnections = connections.toMutableList()
    
    // Add a connection from node2 back to node0 to create a cycle
    cycleConnections.add(
        Connection(
            fromPin = Connection.PinConnectionItem(
                parentId = 2,
                pinId = 0
            ),
            toPin = Connection.PinConnectionItem(
                parentId = 0,
                pinId = 0
            )
        )
    )
    
    try {
        useCase.invoke(cycleNodes, cycleConnections)
        println("Test failed: Expected cycle detection exception was not thrown")
    } catch (e: IllegalStateException) {
        if (e.message?.contains("Cycle detected") == true) {
            println("Test passed: Cycle detection works correctly")
        } else {
            println("Test failed: Unexpected exception: ${e.message}")
            e.printStackTrace()
        }
    } catch (e: Exception) {
        println("Test failed: Unexpected exception: ${e.message}")
        e.printStackTrace()
    }
}