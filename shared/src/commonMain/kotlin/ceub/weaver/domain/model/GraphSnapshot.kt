package ceub.weaver.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GraphSnapshot(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val selectedDatabase: String = "PostgreSQL",
)

@Serializable
data class GraphNode(
    val id: String,
    val definition: String,
    val x: Float,
    val y: Float,
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val columns: List<ColumnDefSnapshot> = emptyList(),
)

@Serializable
data class ColumnDefSnapshot(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean = false,
    val isNotNull: Boolean = false,
    val isUnique: Boolean = false,
)

@Serializable
data class GraphEdge(
    val fromNodeId: String,
    val fromPort: String,
    val toNodeId: String,
    val toPort: String,
)
