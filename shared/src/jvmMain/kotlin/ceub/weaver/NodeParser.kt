package ceub.weaver

import com.martmists.compose.grapheditor.data.Connection
import com.martmists.compose.grapheditor.data.Graph
import com.martmists.compose.grapheditor.data.Node

object NodeParser {
    fun generate(
        graph: Graph<*>,
        nodeColumns: Map<Any, List<ColumnDef>>,
        database: String,
    ): String {
        val dialect = when (database) {
            "MySQL" -> SqlDialect.MYSQL
            else -> SqlDialect.POSTGRESQL
        }
        val sb = StringBuilder()

        val enumNodes = graph.nodes.filter { it.definition.name == "Enum" }
        val tableNodes = graph.nodes.filter { it.definition.name == "Table" }

        val tableByName = mutableMapOf<String, Node<*>>()
        for (node in tableNodes) {
            val name = node.properties["Name"] as? String ?: "new_table"
            tableByName[name] = node
        }

        val fkFromConnections = mutableMapOf<Node<*>, MutableList<Node<*>>>()
        for (conn in graph.connections) {
            val from = conn.fromNode
            val to = conn.toNode
            if (from.definition.name == "Table" && to.definition.name == "Table") {
                fkFromConnections.getOrPut(from) { mutableListOf() }.add(to)
            }
        }

        for (node in enumNodes) {
            val name = node.properties["Name"] as? String ?: "new_enum"
            val values = (node.properties["Values"] as? String ?: "")
                .split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (values.isEmpty()) continue
            sb.appendLine("CREATE TYPE ${dialect.quote(name)} AS ENUM (${values.joinToString(", ") { dialect.quote(it) }});")
            sb.appendLine()
        }

        for (node in tableNodes) {
            val tableName = node.properties["Name"] as? String ?: "new_table"
            val schema = node.properties["Schema"] as? String ?: "public"
            val comment = node.properties["Comment"] as? String ?: ""
            val columns = nodeColumns[node] ?: emptyList()
            val refTables = fkFromConnections[node] ?: emptyList()

            val schemaPrefix = if (schema.isNotBlank() && schema != "public") "${dialect.quote(schema)}." else ""

            sb.append("CREATE TABLE ${schemaPrefix}${dialect.quote(tableName)} (\n")

            val colLines = mutableListOf<String>()

            for (col in columns) {
                val parts = mutableListOf("  ${dialect.quote(col.name)} ${col.type}")
                if (col.isNotNull) parts.add("NOT NULL")
                if (col.isUnique) parts.add("UNIQUE")
                colLines.add(parts.joinToString(" "))
            }

            val pkColumns = columns.filter { it.isPrimaryKey }
            if (pkColumns.isNotEmpty()) {
                colLines.add("  PRIMARY KEY (${pkColumns.joinToString(", ") { dialect.quote(it.name) }})")
            }

            sb.append(colLines.joinToString(",\n"))
            sb.append("\n);\n")

            if (comment.isNotBlank()) {
                sb.appendLine("COMMENT ON TABLE ${schemaPrefix}${dialect.quote(tableName)} IS ${dialect.quote(comment)};")
            }
            sb.appendLine()

            for (refTable in refTables) {
                val refName = refTable.properties["Name"] as? String ?: "new_table"
                val refSchema = refTable.properties["Schema"] as? String ?: "public"
                val refPkColumns = (nodeColumns[refTable] ?: emptyList()).filter { it.isPrimaryKey }
                if (refPkColumns.isEmpty()) continue
                val refCol = refPkColumns.first()
                val expectedFkName = "${refName.lowercase()}_${refCol.name}"
                val fkColumn = columns.find { it.name == refCol.name || it.name == expectedFkName }
                val fkColName = fkColumn?.name ?: expectedFkName
                val refPrefix = if (refSchema.isNotBlank() && refSchema != "public") "${dialect.quote(refSchema)}." else ""
                if (fkColumn != null) {
                    sb.appendLine("ALTER TABLE ${schemaPrefix}${dialect.quote(tableName)}")
                    sb.appendLine("  ADD FOREIGN KEY (${dialect.quote(fkColName)})")
                    sb.appendLine("  REFERENCES ${refPrefix}${dialect.quote(refName)} (${dialect.quote(refCol.name)});")
                } else {
                    sb.appendLine("-- FK: ${dialect.quote(tableName)}.${dialect.quote(expectedFkName)} -> ${refPrefix}${dialect.quote(refName)}.${dialect.quote(refCol.name)}")
                }
                sb.appendLine()
            }
        }

        return sb.toString().trimEnd()
    }
}

private sealed class SqlDialect {
    abstract fun quote(id: String): String

    data object POSTGRESQL : SqlDialect() {
        override fun quote(id: String): String {
            return if (id.matches(Regex("^[a-z_][a-z0-9_]*$")) && !isReservedWord(id))
                id else "\"${id.replace("\"", "\"\"")}\""
        }
    }

    data object MYSQL : SqlDialect() {
        override fun quote(id: String): String = "`${id.replace("`", "``")}`"
    }

    companion object {
        private val reservedWords = setOf(
            "select", "from", "where", "table", "create", "alter", "add", "drop",
            "index", "primary", "key", "foreign", "not", "null", "unique", "check",
            "default", "constraint", "references", "type", "enum", "serial", "boolean",
            "integer", "bigint", "smallint", "numeric", "varchar", "text", "date",
            "timestamp", "user", "public", "order", "group", "by", "as", "in", "is",
            "and", "or", "true", "false",
        )

        fun isReservedWord(id: String): Boolean = id.lowercase() in reservedWords
    }
}
