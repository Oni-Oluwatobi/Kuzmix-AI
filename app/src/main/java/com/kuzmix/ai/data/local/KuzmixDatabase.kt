package com.kuzmix.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kuzmix.ai.data.local.dao.ConceptDao
import com.kuzmix.ai.data.local.dao.ConversationDao
import com.kuzmix.ai.data.local.dao.MessageDao
import com.kuzmix.ai.data.local.entity.ConceptEntity
import com.kuzmix.ai.data.local.entity.ConceptFtsEntity
import com.kuzmix.ai.data.local.entity.ConversationEntity
import com.kuzmix.ai.data.local.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ConceptEntity::class,
        ConceptFtsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KuzmixDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun conceptDao(): ConceptDao

    companion object {
        @Volatile
        private var instance: KuzmixDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `concepts` (" +
                        "`id` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`domain` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`definition` TEXT NOT NULL, " +
                        "`aliasesCsv` TEXT NOT NULL, " +
                        "`keywordsCsv` TEXT NOT NULL, " +
                        "`relatedCsv` TEXT NOT NULL, " +
                        "`examplesCsv` TEXT NOT NULL, " +
                        "`misconceptionsCsv` TEXT NOT NULL, " +
                        "`formula` TEXT NOT NULL, " +
                        "`confidence` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `concepts_fts` " +
                        "USING fts4(`conceptId`, `name`, `definition`, `aliasesCsv`)"
                )
            }
        }

        fun get(context: Context): KuzmixDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KuzmixDatabase::class.java,
                    "kuzmix.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
