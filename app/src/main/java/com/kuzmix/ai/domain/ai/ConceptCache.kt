package com.kuzmix.ai.domain.ai

/**
 * Bounded LRU cache of recently used concepts (spec 4.5).
 * Access-order LinkedHashMap; eldest entries are evicted past capacity.
 * Capacity is supplied by AppContainer based on the device RAM class
 * (spec 1: ~15 on low-RAM devices, 30-50 on comfortable ones).
 */
class ConceptCache(private val capacity: Int) {

    private val map = object : LinkedHashMap<String, ConceptRecord>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ConceptRecord>?): Boolean =
            size > capacity
    }

    val size: Int get() = map.size

    @Synchronized
    fun get(id: String): ConceptRecord? = map[id]

    @Synchronized
    fun contains(id: String): Boolean = map.containsKey(id)

    @Synchronized
    fun put(id: String, concept: ConceptRecord) {
        map[id] = concept
    }

    @Synchronized
    fun getOrPut(id: String, loader: () -> ConceptRecord): ConceptRecord =
        map.getOrPut(id) { loader() }

    @Synchronized
    fun clear() = map.clear()
}
