package com.seunome.finlo

import com.seunome.finlo.core.util.UuidV7
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UuidV7Test {

    @Test
    fun testRN08_uuidV7StructureAndVersion() {
        val id = UuidV7.generate()
        val parsed = UUID.fromString(id)

        assertEquals("UUIDv7 must have version 7", 7, parsed.version())
        assertEquals("UUIDv7 must have RFC variant 2", 2, parsed.variant())
        assertEquals(36, id.length)
        assertEquals('-', id[8])
        assertEquals('-', id[13])
        assertEquals('-', id[18])
        assertEquals('-', id[23])
        assertEquals('7', id[14])
    }

    @Test
    fun testRN08_uuidV7TimeOrderingMonotonicity() {
        val t1 = 1_000_000_000L
        val t2 = 2_000_000_000L

        val id1 = UuidV7.generate(t1)
        val id2 = UuidV7.generate(t2)

        assertTrue("Earlier timestamp must precede later timestamp lexicographically", id1 < id2)
    }

    @Test
    fun testRN08_uniquenessAcrossCalls() {
        val generated = (1..100).map { UuidV7.generate() }.toSet()
        assertEquals("All generated UUIDs must be unique", 100, generated.size)
    }
}
