package ceub.weaver

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import ceub.weaver.domain.model.ColumnDefSnapshot
import ceub.weaver.domain.model.GraphEdge
import ceub.weaver.domain.model.GraphNode
import ceub.weaver.domain.model.GraphSnapshot
import com.martmists.compose.grapheditor.data.Graph
import com.martmists.compose.grapheditor.data.Node
import com.martmists.compose.grapheditor.data.NodeDefinition
import com.martmists.compose.grapheditor.data.property.BoolProperty
import com.martmists.compose.grapheditor.data.property.ChoiceProperty
import com.martmists.compose.grapheditor.data.property.FloatProperty
import com.martmists.compose.grapheditor.data.property.IntProperty
import com.martmists.compose.grapheditor.data.property.StringProperty

object GraphSerializer {

    fun Graph<*>.toSnapshot(
        nodeColumns: Map<Any, List<ColumnDef>>,
        selectedDatabase: String,
    ): GraphSnapshot {
        val snapshotNodes = nodes.map { node ->
            GraphNode(
                id = node.id,
                definition = node.definition.name,
                x = node.position.x,
                y = node.position.y,
                name = node.name,
                properties = node.definition.properties.associate { prop ->
                    prop.name to (node.properties[prop.name]?.toString() ?: prop.default.toString())
                },
                columns = (nodeColumns[node] ?: emptyList()).map { col ->
                    ColumnDefSnapshot(
                        name = col.name,
                        type = col.type,
                        isPrimaryKey = col.isPrimaryKey,
                        isNotNull = col.isNotNull,
                        isUnique = col.isUnique,
                    )
                },
            )
        }

        val snapshotEdges = connections.map { conn ->
            GraphEdge(
                fromNodeId = conn.fromNode.id,
                fromPort = conn.fromPort.name,
                toNodeId = conn.toNode.id,
                toPort = conn.toPort.name,
            )
        }

        return GraphSnapshot(
            nodes = snapshotNodes,
            edges = snapshotEdges,
            selectedDatabase = selectedDatabase,
        )
    }

    fun GraphSnapshot.restore(
        graph: Graph<*>,
        definitions: List<NodeDefinition>,
        nodeColumns: MutableMap<Any, MutableList<ColumnDef>>,
    ) {
        nodeColumns.clear()

        val defByName = definitions.associateBy { it.name }
        val idToNode = mutableMapOf<String, Node<*>>()

        for (sn in nodes) {
            val def = defByName[sn.definition] ?: continue
            val node = graph.addNode(def, Offset(sn.x, sn.y), sn.id)
            node.name = sn.name
            idToNode[sn.id] = node

            for (prop in def.properties) {
                val value = sn.properties[prop.name]
                if (value != null) {
                    try {
                        when (prop) {
                            is StringProperty -> node.setProperty(prop.name, value)
                            is BoolProperty -> node.setProperty(prop.name, value.toBooleanStrictOrNull() ?: false)
                            is IntProperty -> node.setProperty(prop.name, value.toIntOrNull() ?: 0)
                            is FloatProperty -> node.setProperty(prop.name, value.toFloatOrNull() ?: 0f)
                            is ChoiceProperty -> node.setProperty(prop.name, value)
                        }
                    } catch (_: Exception) { }
                }
            }

            val colList = mutableStateListOf<ColumnDef>()
            for (col in sn.columns) {
                colList.add(ColumnDef(col.name, col.type, col.isPrimaryKey, col.isNotNull, col.isUnique))
            }
            nodeColumns[node] = colList
        }

        for (edge in edges) {
            val fromNode = idToNode[edge.fromNodeId] ?: continue
            val toNode = idToNode[edge.toNodeId] ?: continue
            val fromPort = fromNode.definition.outputPorts.find { it.name == edge.fromPort } ?: continue
            val toPort = toNode.definition.inputPorts.find { it.name == edge.toPort } ?: continue
            graph.tryConnect(fromNode, fromPort, toNode, toPort)
        }
    }
}
