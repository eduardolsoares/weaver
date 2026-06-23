package com.martmists.compose.grapheditor.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.martmists.compose.grapheditor.compose.internal.NODE_TITLE_HEIGHT
import com.martmists.compose.grapheditor.compose.internal.NodeScope
import com.martmists.compose.grapheditor.compose.internal.PORT_INSET
import com.martmists.compose.grapheditor.compose.internal.PORT_OFFSET
import com.martmists.compose.grapheditor.compose.internal.PORT_PADDING
import com.martmists.compose.grapheditor.compose.internal.PORT_SIZE
import com.martmists.compose.grapheditor.data.property.PropertyDefinition
import java.util.UUID

class Node<T>(
    val graph: Graph<T>,
    val definition: NodeDefinition,
    position: Offset = Offset.Zero,
    val id: String = UUID.randomUUID().toString(),
) {
    var position by mutableStateOf(position)
    var name by mutableStateOf(definition.name)

    val properties: Map<String, Any?>
        field = mutableStateMapOf<String, Any?>()

    var customData: T? = null

    val connections: List<Connection>
        get() = graph.connections.filter { it.toNode === this || it.fromNode === this }

    // Utilities for rendering
    context(scope: NodeScope)
    internal fun portOffset(index: Int, output: Boolean) = Offset(
        position.x + if (output) scope.measureNodeWidth(this) - PORT_INSET else PORT_INSET,
        position.y + NODE_TITLE_HEIGHT + PORT_OFFSET + (2 * PORT_SIZE + 2 * PORT_PADDING) * (index + 0.5f)
    )

    var computedSize: Size? = null

    context(scope: NodeScope)
    fun size(): Size {
        return computedSize ?: run {
            val asOff = portOffset(maxOf(definition.inputPorts.lastIndex, definition.outputPorts.lastIndex), true) - position
            computedSize = Size(
                asOff.x + PORT_INSET,
                asOff.y + (2 * PORT_SIZE + 2 * PORT_PADDING) * 0.5f
            )
            computedSize!!
        }
    }

    init {
        for (prop in definition.properties) {
            properties[prop.name] = prop.default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Any> getProperty(name: String): V = properties[name] as V

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> setProperty(name: String, value: T) {
        val def = definition.properties.first { it.name == name } as PropertyDefinition<T>
        if (def.validate(value)) {
            properties[name] = value
        }
    }
}
