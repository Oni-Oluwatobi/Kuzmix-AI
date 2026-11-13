package com.kuzmix.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuzmix.ai.data.local.entity.ConceptEntity
import com.kuzmix.ai.data.local.entity.ConceptFtsEntity

@Dao
interface ConceptDao {

    @Query("SELECT conceptId FROM concepts_fts WHERE concepts_fts MATCH :match LIMIT :limit")
    suspend fun searchFts(match: String, limit: Int): List<String>

    @Query("SELECT * FROM concepts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE id = :id")
    suspend fun getById(id: String): ConceptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<ConceptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(rows: List<ConceptFtsEntity>)

    @Query("DELETE FROM concepts")
    suspend fun deleteAllConcepts()

    @Query("DELETE FROM concepts_fts")
    suspend fun deleteAllFts()

    @Query("SELECT COUNT(*) FROM concepts")
    suspend fun count(): Int
}
