package ceub.weaver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DatabaseTypesTest {

    @Test
    fun `database names are correct`() {
        assertEquals(listOf("PostgreSQL", "MySQL"), databaseNames)
    }

    @Test
    fun `postgresql contains expected types`() {
        val types = databaseTypes["PostgreSQL"] ?: error("PostgreSQL missing")
        assertTrue("integer" in types)
        assertTrue("bigint" in types)
        assertTrue("varchar" in types)
        assertTrue("text" in types)
        assertTrue("boolean" in types)
        assertTrue("timestamp" in types)
        assertTrue("uuid" in types)
        assertTrue("jsonb" in types)
    }

    @Test
    fun `mysql contains expected types`() {
        val types = databaseTypes["MySQL"] ?: error("MySQL missing")
        assertTrue("int" in types)
        assertTrue("bigint" in types)
        assertTrue("varchar" in types)
        assertTrue("text" in types)
        assertTrue("boolean" in types)
        assertTrue("timestamp" in types)
        assertTrue("json" in types)
        assertTrue("blob" in types)
    }

    @Test
    fun `postgresql type count`() {
        val types = databaseTypes["PostgreSQL"] ?: error("PostgreSQL missing")
        assertEquals(18, types.size)
    }

    @Test
    fun `mysql type count`() {
        val types = databaseTypes["MySQL"] ?: error("MySQL missing")
        assertEquals(17, types.size)
    }

    @Test
    fun `postgresql to mysql mapping covers all pg types`() {
        val mapping = typeMapping["PostgreSQL->MySQL"] ?: error("PG->MySQL mapping missing")
        val pgTypes = databaseTypes["PostgreSQL"] ?: error("PostgreSQL missing")
        for (pgType in pgTypes) {
            assertTrue(pgType in mapping, "PG type '$pgType' should have a MySQL mapping")
        }
    }

    @Test
    fun `mysql to postgresql mapping covers all mysql types`() {
        val mapping = typeMapping["MySQL->PostgreSQL"] ?: error("MySQL->PG mapping missing")
        val mysqlTypes = databaseTypes["MySQL"] ?: error("MySQL missing")
        for (mysqlType in mysqlTypes) {
            assertTrue(mysqlType in mapping, "MySQL type '$mysqlType' should have a PG mapping")
        }
    }

    @Test
    fun `unmappable postgresql types produce empty mysql`() {
        val mapping = typeMapping["PostgreSQL->MySQL"] ?: error("PG->MySQL mapping missing")
        assertEquals("", mapping["interval"], "interval has no MySQL equivalent")
        assertEquals("", mapping["uuid"], "uuid has no MySQL equivalent")
    }

    @Test
    fun `mysql types map to expected postgresql equivalents`() {
        val mapping = typeMapping["MySQL->PostgreSQL"] ?: error("MySQL->PG mapping missing")
        assertEquals("smallint", mapping["tinyint"])
        assertEquals("varchar", mapping["char"])
        assertEquals("timestamp", mapping["datetime"])
    }

    @Test
    fun `double precision round trips through mysql`() {
        val pgToMy = typeMapping["PostgreSQL->MySQL"] ?: error("PG->MySQL missing")
        val myToPg = typeMapping["MySQL->PostgreSQL"] ?: error("MySQL->PG missing")
        assertEquals("double", pgToMy["double precision"])
        assertEquals("double precision", myToPg["double"])
    }

    @Test
    fun `boolean mapping is identity`() {
        val pgToMy = typeMapping["PostgreSQL->MySQL"] ?: error("PG->MySQL missing")
        val myToPg = typeMapping["MySQL->PostgreSQL"] ?: error("MySQL->PG missing")
        assertEquals("boolean", pgToMy["boolean"])
        assertEquals("boolean", myToPg["boolean"])
    }
}

class ColumnDefTest {

    @Test
    fun `default values`() {
        val col = ColumnDef("id", "integer")
        assertEquals("id", col.name)
        assertEquals("integer", col.type)
        assertFalse(col.isPrimaryKey)
        assertFalse(col.isNotNull)
        assertFalse(col.isUnique)
    }

    @Test
    fun `primary key flag`() {
        val col = ColumnDef("id", "integer", isPrimaryKey = true)
        assertTrue(col.isPrimaryKey)
    }

    @Test
    fun `not null flag`() {
        val col = ColumnDef("email", "varchar", isNotNull = true)
        assertTrue(col.isNotNull)
    }

    @Test
    fun `unique flag`() {
        val col = ColumnDef("sku", "varchar", isUnique = true)
        assertTrue(col.isUnique)
    }

    @Test
    fun `copy with modified properties`() {
        val col = ColumnDef("id", "integer")
        val modified = col.copy(type = "bigint", isPrimaryKey = true)
        assertEquals("bigint", modified.type)
        assertTrue(modified.isPrimaryKey)
        assertEquals("id", modified.name)
        assertFalse(modified.isNotNull)
    }
}
