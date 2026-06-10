package com.martmists.compose.grapheditor.data

sealed interface GraphEvent {
    val isUndo: Boolean

    data class NodeCreate(val node: Node<*>, override val isUndo: Boolean) : GraphEvent
    data class NodeDelete(val node: Node<*>, override val isUndo: Boolean) : GraphEvent
    data class NodePropertyUpdate(val node: Node<*>, val property: String, val oldValue: Any, val newValue: Any, override val isUndo: Boolean) : GraphEvent
    data class ConnectionCreate(val connection: Connection, override val isUndo: Boolean) : GraphEvent
    data class ConnectionDelete(val connection: Connection, override val isUndo: Boolean) : GraphEvent
}
