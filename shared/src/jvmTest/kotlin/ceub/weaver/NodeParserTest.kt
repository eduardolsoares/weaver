package ceub.weaver

import com.martmists.compose.grapheditor.data.*
import com.martmists.compose.grapheditor.data.property.StringProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeParserTest {

    private fun tableDef(
        name: String = "new_table",
        schema: String = "public",
        comment: String = "",
    ) = NodeDefinition(
        name = "Table",
        ports = listOf(
            PortDefinition("FK in", PortKind.Input, dataType = "table"),
            PortDefinition("FK out", PortKind.Output, dataType = "table"),
        ),
        properties = listOf(
            StringProperty("Name", name),
            StringProperty("Schema", schema),
            StringProperty("Comment", comment),
        ),
    )

    private val enumDef = NodeDefinition(
        name = "Enum",
        ports = listOf(PortDefinition("ref", PortKind.Output, dataType = "enum")),
        properties = listOf(
            StringProperty("Name", "new_enum"),
            StringProperty("Values", ""),
        ),
    )

    // --- Empty / edge cases ---

    @Test
    fun `empty graph`() {
        val graph = Graph<Unit>(emptyList())
        assertEquals("", NodeParser.generate(graph, emptyMap(), "PostgreSQL"))
    }

    @Test
    fun `graph with only note nodes`() {
        val noteDef = NodeDefinition(name = "Note", properties = listOf(StringProperty("Text", "")))
        val graph = Graph<Unit>(listOf(noteDef))
        graph.addNode(noteDef)
        assertEquals("", NodeParser.generate(graph, emptyMap(), "PostgreSQL"))
    }

    @Test
    fun `table node not in nodeColumns`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val result = NodeParser.generate(graph, emptyMap(), "PostgreSQL")
        assertEquals("CREATE TABLE users (\n\n);", result)
    }

    // --- PostgreSQL tables ---

    @Test
    fun `table with no columns`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val result = NodeParser.generate(graph, mapOf(table to emptyList()), "PostgreSQL")
        assertEquals("CREATE TABLE users (\n\n);", result)
    }

    @Test
    fun `table with columns`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val cols = listOf(ColumnDef("id", "integer"), ColumnDef("email", "varchar"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE users (
            |  id integer,
            |  email varchar
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `primary key constraint`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val cols = listOf(ColumnDef("id", "integer", isPrimaryKey = true))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE users (
            |  id integer,
            |  PRIMARY KEY (id)
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `not null constraint`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val cols = listOf(ColumnDef("email", "varchar", isNotNull = true))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE users (
            |  email varchar NOT NULL
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `unique constraint`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "products")
        val cols = listOf(ColumnDef("sku", "varchar", isUnique = true))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE products (
            |  sku varchar UNIQUE
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `combined constraints`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val cols = listOf(
            ColumnDef("id", "integer", isPrimaryKey = true, isNotNull = true),
            ColumnDef("email", "varchar", isNotNull = true, isUnique = true),
            ColumnDef("name", "text"),
        )
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE users (
            |  id integer NOT NULL,
            |  email varchar NOT NULL UNIQUE,
            |  name text,
            |  PRIMARY KEY (id)
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `composite primary key`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "order_items")
        val cols = listOf(
            ColumnDef("order_id", "integer", isPrimaryKey = true),
            ColumnDef("product_id", "integer", isPrimaryKey = true),
        )
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE order_items (
            |  order_id integer,
            |  product_id integer,
            |  PRIMARY KEY (order_id, product_id)
            |);
            """.trimMargin("|"),
            result,
        )
    }

    // --- Schema and comment ---

    @Test
    fun `custom schema prefix`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "orders")
        graph.updateProperty(table, "Schema", "sales")
        val cols = listOf(ColumnDef("id", "integer"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE sales.orders (
            |  id integer
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `public schema omits prefix`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        graph.updateProperty(table, "Schema", "public")
        val cols = listOf(ColumnDef("id", "integer"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertFalse(result.contains("public."), "Schema prefix should be omitted for 'public'")
    }

    @Test
    fun `table comment`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        graph.updateProperty(table, "Comment", "User accounts")
        val cols = listOf(ColumnDef("id", "integer"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertTrue(result.contains("""COMMENT ON TABLE users IS "User accounts""""))
    }

    @Test
    fun `empty comment omitted`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        graph.updateProperty(table, "Comment", "")
        val cols = listOf(ColumnDef("id", "integer"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertFalse(result.contains("COMMENT ON"))
    }

    // --- Identifier quoting ---

    @Test
    fun `reserved word table name`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "user")
        val cols = listOf(ColumnDef("id", "integer"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertTrue(result.contains("\"user\""), "Reserved word 'user' should be quoted")
    }

    @Test
    fun `reserved word column name`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val cols = listOf(ColumnDef("select", "integer"), ColumnDef("from", "text"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertTrue(result.contains("\"select\""), "Reserved word column should be quoted")
        assertTrue(result.contains("\"from\""), "Reserved word column should be quoted")
    }

    @Test
    fun `mixed case column name`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def)
        graph.updateProperty(table, "Name", "users")
        val cols = listOf(ColumnDef("fullName", "varchar"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "PostgreSQL")
        assertEquals(
            """
            |CREATE TABLE users (
            |  "fullName" varchar
            |);
            """.trimMargin("|"),
            result,
        )
    }

    // --- Enums ---

    @Test
    fun `enum type`() {
        val graph = Graph<Unit>(listOf(enumDef))
        val e = graph.addNode(enumDef)
        graph.updateProperty(e, "Name", "mood")
        graph.updateProperty(e, "Values", "happy,sad,neutral")
        val result = NodeParser.generate(graph, emptyMap(), "PostgreSQL")
        assertEquals(
            """
            |CREATE TYPE mood AS ENUM (happy, sad, neutral);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `enum with empty values`() {
        val graph = Graph<Unit>(listOf(enumDef))
        val e = graph.addNode(enumDef)
        graph.updateProperty(e, "Name", "empty")
        graph.updateProperty(e, "Values", "")
        assertEquals("", NodeParser.generate(graph, emptyMap(), "PostgreSQL"))
    }

    @Test
    fun `enum with only commas`() {
        val graph = Graph<Unit>(listOf(enumDef))
        val e = graph.addNode(enumDef)
        graph.updateProperty(e, "Name", "blank")
        graph.updateProperty(e, "Values", ",,,")
        assertEquals("", NodeParser.generate(graph, emptyMap(), "PostgreSQL"))
    }

    @Test
    fun `multiple enums`() {
        val graph = Graph<Unit>(listOf(enumDef))
        val e1 = graph.addNode(enumDef)
        graph.updateProperty(e1, "Name", "color")
        graph.updateProperty(e1, "Values", "red,green,blue")
        val e2 = graph.addNode(enumDef)
        graph.updateProperty(e2, "Name", "size")
        graph.updateProperty(e2, "Values", "small,medium,large")
        val result = NodeParser.generate(graph, emptyMap(), "PostgreSQL")
        assertEquals(
            """
            |CREATE TYPE color AS ENUM (red, green, blue);
            |
            |CREATE TYPE size AS ENUM (small, medium, large);
            """.trimMargin("|"),
            result,
        )
    }

    // --- Mixed enum + table ---

    @Test
    fun `enum before table output`() {
        val tDef = tableDef()
        val graph = Graph<Unit>(listOf(tDef, enumDef))
        val t = graph.addNode(tDef)
        graph.updateProperty(t, "Name", "users")
        graph.updateProperty(t, "Values", "")
        val e = graph.addNode(enumDef)
        graph.updateProperty(e, "Name", "user_status")
        graph.updateProperty(e, "Values", "active,inactive")
        val cols = listOf(ColumnDef("status", "user_status"))
        val result = NodeParser.generate(graph, mapOf(t to cols), "PostgreSQL")
        assertTrue(result.startsWith("CREATE TYPE"))
        assertTrue(result.contains("CREATE TABLE"))
    }

    // --- Foreign keys ---

    @Test
    fun `foreign key between tables`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())

        val ordersCols = listOf(ColumnDef("id", "integer", isPrimaryKey = true))
        val usersCols = listOf(ColumnDef("id", "integer", isPrimaryKey = true))
        val result = NodeParser.generate(graph, mapOf(orders to ordersCols, users to usersCols), "PostgreSQL")
        assertTrue(result.startsWith("CREATE TABLE orders"))
        assertTrue(result.contains("ALTER TABLE orders"))
        assertTrue(result.contains("ADD FOREIGN KEY (id)"))
        assertTrue(result.contains("REFERENCES users (id)"))
    }

    @Test
    fun `fk with no pk on target omitted entirely`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())

        val result = NodeParser.generate(
            graph, mapOf(
                orders to listOf(ColumnDef("id", "integer")),
                users to listOf(ColumnDef("email", "varchar")),
            ), "PostgreSQL",
        )
        assertEquals(
            """
            |CREATE TABLE orders (
            |  id integer
            |);
            |
            |CREATE TABLE users (
            |  email varchar
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `fk uses existing matching column`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())

        val ordersCols = listOf(ColumnDef("users_id", "integer"))
        val usersCols = listOf(ColumnDef("id", "integer", isPrimaryKey = true))
        val result = NodeParser.generate(graph, mapOf(orders to ordersCols, users to usersCols), "PostgreSQL")
        assertTrue(result.contains("ADD FOREIGN KEY (users_id)"))
    }

    @Test
    fun `multiple fks from same table`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        val products = graph.addNode(def).also { graph.updateProperty(it, "Name", "products") }

        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())
        graph.tryConnect(orders, orders.definition.outputPorts.first(), products, products.definition.inputPorts.first())

        val shared = listOf(ColumnDef("id", "integer", isPrimaryKey = true))
        val result = NodeParser.generate(graph, mapOf(orders to shared, users to shared, products to shared), "PostgreSQL")
        assertTrue(result.contains("ADD FOREIGN KEY (id)"), "FK uses ref col name 'id' when source has matching col")
    }

    @Test
    fun `fk with custom schema on target`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also {
            graph.updateProperty(it, "Name", "users")
            graph.updateProperty(it, "Schema", "auth")
        }
        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())

        val cols = listOf(ColumnDef("id", "integer", isPrimaryKey = true))
        val result = NodeParser.generate(graph, mapOf(orders to cols, users to cols), "PostgreSQL")
        assertTrue(result.contains("REFERENCES auth.users"))
    }

    // --- MySQL dialect ---

    @Test
    fun `mysql backtick quoting`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        val cols = listOf(ColumnDef("id", "int"))
        val result = NodeParser.generate(graph, mapOf(table to cols), "MySQL")
        assertEquals(
            """
            |CREATE TABLE `users` (
            |  `id` int
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `mysql composite pk`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val table = graph.addNode(def).also { graph.updateProperty(it, "Name", "order_items") }
        val cols = listOf(
            ColumnDef("order_id", "int", isPrimaryKey = true),
            ColumnDef("product_id", "int", isPrimaryKey = true),
        )
        val result = NodeParser.generate(graph, mapOf(table to cols), "MySQL")
        assertEquals(
            """
            |CREATE TABLE `order_items` (
            |  `order_id` int,
            |  `product_id` int,
            |  PRIMARY KEY (`order_id`, `product_id`)
            |);
            """.trimMargin("|"),
            result,
        )
    }

    @Test
    fun `mysql fk`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())

        val cols = listOf(ColumnDef("id", "int", isPrimaryKey = true))
        val result = NodeParser.generate(graph, mapOf(orders to cols, users to cols), "MySQL")
        assertTrue(result.contains("ADD FOREIGN KEY (`id`)"))
        assertTrue(result.contains("REFERENCES `users` (`id`)"))
    }

    @Test
    fun `mysql fk with no pk on target omitted`() {
        val def = tableDef()
        val graph = Graph<Unit>(listOf(def))
        val orders = graph.addNode(def).also { graph.updateProperty(it, "Name", "orders") }
        val users = graph.addNode(def).also { graph.updateProperty(it, "Name", "users") }
        graph.tryConnect(orders, orders.definition.outputPorts.first(), users, users.definition.inputPorts.first())

        val result = NodeParser.generate(
            graph, mapOf(
                orders to listOf(ColumnDef("id", "int")),
                users to listOf(ColumnDef("email", "varchar")),
            ), "MySQL",
        )
        assertEquals(
            """
            |CREATE TABLE `orders` (
            |  `id` int
            |);
            |
            |CREATE TABLE `users` (
            |  `email` varchar
            |);
            """.trimMargin("|"),
            result,
        )
    }

    // --- MySQL enum (same as PG — dialect affects quoting only) ---

    @Test
    fun `mysql enum uses backtick quoting`() {
        val graph = Graph<Unit>(listOf(enumDef))
        val e = graph.addNode(enumDef)
        graph.updateProperty(e, "Name", "mood")
        graph.updateProperty(e, "Values", "happy,sad")
        val result = NodeParser.generate(graph, emptyMap(), "MySQL")
        assertTrue(result.startsWith("CREATE TYPE `mood`"))
        assertTrue(result.contains("(`happy`, `sad`)"))
    }
}
