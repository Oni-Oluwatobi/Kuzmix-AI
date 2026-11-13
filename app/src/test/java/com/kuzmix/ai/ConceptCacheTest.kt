package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.ConceptCache
import com.kuzmix.ai.domain.ai.ConceptRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConceptCacheTest {

    private fun concept(id: String): ConceptRecord = ConceptRecord(
        id = id,
        type = "CONCEPT",
        domain = "biology",
        name = "name-$id",
        aliases = emptyList(),
        definition = "definition-$id",
        keywords = emptyList(),
        related = emptyList(),
        examples = emptyList(),
        misconceptions = emptyList(),
        formula = "",
        confidence = "HIGH"
    )

    @Test
    fun capacityIsEnforced_inserting40LeavesMostRecent30() {
        val cache = ConceptCache(capacity = 30)
        for (i in 1..40) {
            val c = concept("c$i")
            cache.put(c.id, c)
        }

        assertEquals(30, cache.size)
        for (i in 1..10) {
            assertNull("c$i should have been evicted", cache.get("c$i"))
        }
        for (i in 11..40) {
            assertNotNull("c$i should still be cached", cache.get("c$i"))
        }
    }

    @Test
    fun accessRefreshesRecency_evictsLeastRecentlyUsed() {
        val cache = ConceptCache(capacity = 3)
        cache.put("a", concept("a"))
        cache.put("b", concept("b"))
        cache.put("c", concept("c"))

        cache.get("a") // refresh "a" — now "b" is eldest

        cache.put("d", concept("d"))

        assertNotNull(cache.get("a"))
        assertNull(cache.get("b"))
        assertNotNull(cache.get("c"))
        assertNotNull(cache.get("d"))
        assertEquals(3, cache.size)
    }

    @Test
    fun getOrPut_reusesStoredValue() {
        val cache = ConceptCache(capacity = 2)
        cache.put("x", concept("x"))

        var loads = 0
        val first = cache.getOrPut("x") { loads++; concept("x") }
        val second = cache.getOrPut("y") { loads++; concept("y") }

        assertEquals("name-x", first.name)
        assertEquals("name-y", second.name)
        assertEquals(1, loads)
    }
}
