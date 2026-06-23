package com.martmists.compose.grapheditor.data

data class Connection(
    val fromNode: Node<*>,
    val fromPort: PortDefinition,
    val toNode: Node<*>,
    val toPort: PortDefinition,
)
