package com.martmists.compose.grapheditor.data

import com.martmists.compose.grapheditor.data.property.PropertyDefinition
import kotlinx.serialization.Serializable

@Serializable
class NodeDefinition(
    val name: String,
    val properties: List<PropertyDefinition<*>> = emptyList(),
    val ports: List<PortDefinition> = emptyList(),
) {
    val inputPorts get() = ports.filter { it.kind == PortKind.Input }
    val outputPorts get() = ports.filter { it.kind == PortKind.Output }
}
