package com.martmists.compose.grapheditor.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(DelicateCoroutinesApi::class)
class Graph<T>(
    val definitions: List<NodeDefinition>,
    val allowCycles: Boolean = true,
    eventMaxSize: Int = Channel.UNLIMITED,
) {
    val nodes: List<Node<T>>
        field = mutableStateListOf<Node<T>>()

    val connections: List<Connection>
        field = mutableStateListOf<Connection>()

    private val eventChannel = Channel<GraphEvent>(eventMaxSize)
    val events: Flow<GraphEvent> = eventChannel.receiveAsFlow()

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    fun addNode(definition: NodeDefinition, position: Offset = Offset.Zero, isUndo: Boolean = false): Node<T> {
        val node = Node<T>(this, definition, position)
        addNodeInternal(node, isUndo)
        return node
    }

    fun addNode(definition: NodeDefinition, position: Offset, id: String, isUndo: Boolean = false): Node<T> {
        val node = Node<T>(this, definition, position, id)
        addNodeInternal(node, isUndo)
        return node
    }

    internal fun addNodeInternal(node: Node<T>, isUndo: Boolean = false) {
        nodes.add(node)
        GlobalScope.launch(dispatcher) {
            eventChannel.send(GraphEvent.NodeCreate(node, isUndo))
        }
    }

    fun removeNode(node: Node<T>, isUndo: Boolean = false) {
        disconnectAll(node, isUndo)
        nodes.remove(node)
        GlobalScope.launch(dispatcher) {
            eventChannel.send(GraphEvent.NodeDelete(node, isUndo))
        }
    }

    fun tryConnect(fromNode: Node<*>, fromPort: PortDefinition, toNode: Node<*>, toPort: PortDefinition, isUndo: Boolean = false): Boolean {
        return connect(fromNode, fromPort, toNode, toPort, isUndo) || connect(toNode, toPort, fromNode, fromPort, isUndo)
    }

    private fun connect(fromNode: Node<*>, fromPort: PortDefinition, toNode: Node<*>, toPort: PortDefinition, isUndo: Boolean = false): Boolean {
        if (fromNode == toNode) return false
        if (fromPort.kind != PortKind.Output || toPort.kind != PortKind.Input) return false
        if (fromPort.dataType != toPort.dataType && fromPort.dataType != "any" && toPort.dataType != "any") return false

        if (!allowCycles) {
            fun visit(node: Node<*>, forward: Boolean, visited: MutableSet<Node<*>>): Boolean {
                if (forward) {
                    if (node == fromNode) return true
                } else {
                    if (node == toNode) return true
                }
                visited.add(node)

                val toVisitNext = node.connections.filter {
                    if (forward) it.fromNode == node else it.toNode == node
                }.map {
                    if (forward) it.toNode else it.fromNode
                }.filter {
                    it !in visited
                }

                if (toVisitNext.isEmpty()) return false
                return toVisitNext.any { visit(it, forward, visited) }
            }

            if (visit(toNode, true, mutableSetOf())) return false
            if (visit(fromNode, false, mutableSetOf())) return false
        }


        val connection = Connection(fromNode, fromPort, toNode, toPort)
        if (connection in connections) return false

        val toRemove = connections.filter { it.toNode == toNode && it.toPort == toPort }
        for (item in toRemove) {
            disconnect(item)
        }


        connections.add(connection)
        GlobalScope.launch(dispatcher) {
            eventChannel.send(GraphEvent.ConnectionCreate(connection, isUndo))
        }
        return true
    }

    fun disconnect(connection: Connection, isUndo: Boolean = false) {
        connections.remove(connection)
        GlobalScope.launch(dispatcher) {
            eventChannel.send(GraphEvent.ConnectionDelete(connection, isUndo))
        }
    }

    fun disconnectAll(node: Node<T>, isUndo: Boolean = false) {
        node.connections.forEach {
            disconnect(it, isUndo)
        }
    }

    fun updateProperty(node: Node<T>, property: String, value: Any, isUndo: Boolean = false) {
        val current = node.properties[property] ?: node.definition.properties.firstOrNull { it.name == property }?.default ?: return
        node.setProperty(property, value)
        GlobalScope.launch(dispatcher) {
            eventChannel.send(GraphEvent.NodePropertyUpdate(node, property, current, value, isUndo))
        }
    }
}
