package com.kuzmix.ai.di

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.kuzmix.ai.data.knowledge.KnowledgeCoreConceptSource
import com.kuzmix.ai.data.knowledge.KnowledgeCoreCorpusSource
import com.kuzmix.ai.data.knowledge.KnowledgeCoreStore
import com.kuzmix.ai.data.knowledge.KnowledgeDataSource
import com.kuzmix.ai.data.knowledge.KnowledgeSeeder
import com.kuzmix.ai.data.knowledge.RoomConceptSource
import com.kuzmix.ai.data.knowledge.SeedState
import com.kuzmix.ai.data.local.KuzmixDatabase
import com.kuzmix.ai.data.preferences.UserPreferences
import com.kuzmix.ai.domain.ai.AIRepository
import com.kuzmix.ai.domain.ai.ConceptCache
import com.kuzmix.ai.domain.ai.KnowledgeEngine
import com.kuzmix.ai.domain.ai.LocalAIEngine
import com.kuzmix.ai.domain.ai.ModelFileManager
import com.kuzmix.ai.domain.repository.ChatRepository
import com.kuzmix.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val database = KuzmixDatabase.get(context)
    private val preferences = UserPreferences(context)
    private val knowledgeDataSource = KnowledgeDataSource(context)
    private val appContext = context.applicationContext

    private val conceptCache = ConceptCache(cacheCapacity(context))
    private val seedGate = CompletableDeferred<Unit>()
    private val kbStore = CompletableDeferred<KnowledgeCoreStore?>()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        val seeder = KnowledgeSeeder(
            loadConcepts = { knowledgeDataSource.loadConcepts() },
            dao = database.conceptDao(),
            state = object : SeedState {
                override suspend fun getVersion(): Int = preferences.getSeedVersion()
                override suspend fun setVersion(version: Int) = preferences.setSeedVersion(version)
            }
        )
        appScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                try {
                    val force = preferences.getKnowledgeDbVersion() != KnowledgeCoreStore.DB_VERSION
                    val store = KnowledgeCoreStore.open(appContext, force)
                    if (store != null) preferences.setKnowledgeDbVersion(KnowledgeCoreStore.DB_VERSION)
                    kbStore.complete(store)
                } catch (t: Throwable) {
                    Log.e(TAG, "knowledge core init failed", t)
                    kbStore.complete(null)
                }
                try {
                    seeder.seedIfNeeded()
                } catch (t: Throwable) {
                    Log.e(TAG, "json fallback seed failed", t)
                }
            } finally {
                if (!kbStore.isCompleted) kbStore.complete(null)
                seedGate.complete(Unit)
                Log.i(TAG, "knowledge init finished in ${System.currentTimeMillis() - startedAt}ms")
            }
        }
    }

    private val roomConceptSource = RoomConceptSource(
        dao = database.conceptDao(),
        cache = conceptCache,
        awaitReady = { seedGate.await() }
    )

    private val conceptSource = KnowledgeCoreConceptSource(
        store = { kbStore.await() },
        cache = conceptCache,
        fallback = roomConceptSource
    )

    private val corpusSource = KnowledgeCoreCorpusSource(store = { kbStore.await() })
    private val knowledgeEngine = KnowledgeEngine(knowledgeDataSource.loadShell(), conceptSource, corpusSource)
    private val modelFileManager = ModelFileManager(appContext)
    private val localAIEngine = LocalAIEngine(appContext, modelFileManager) { path ->
        preferences.setModelPath(path)
    }

    init {
        appScope.launch {
            try {
                preferences.getModelPath()?.let { path ->
                    localAIEngine.loadIfPresent(path)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "local model init failed", t)
            }
        }
    }

    val aiRepository = AIRepository(knowledgeEngine, localAIEngine)
    val chatRepository = ChatRepository(database.conversationDao(), database.messageDao())
    val settingsRepository = SettingsRepository(preferences)

    companion object {
        private const val TAG = "KuzmixSeed"

        /** Spec 1: scale cache to the device — 15 on low-RAM, 25 mid, 40 comfortable. */
        fun cacheCapacity(context: Context): Int {
            val am = context.getSystemService(ActivityManager::class.java) ?: return 30
            return when {
                am.isLowRamDevice -> 15
                am.memoryClass <= 128 -> 25
                else -> 40
            }
        }
    }
}
