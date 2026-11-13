# Kuzmix AI — Complete Project Documentation

**Version 1.0.0 · by The Kreadive Galaxy (Oni Oluwatobi)**
**Document type:** Full technical & structural reference — everything known about the project.
**Location:** `Desktop\My Products\Kuzmix AI`
**Companion document:** `docs/USER_MANUAL.md` (end-user manual).

---

## Table of Contents

1. [What This Project Is](#1-what-this-project-is)
2. [Design Philosophy & Guarantees](#2-design-philosophy--guarantees)
3. [Repository Structure](#3-repository-structure)
4. [Technology Stack](#4-technology-stack)
5. [Build System & Release Process](#5-build-system--release-process)
6. [App Architecture (MVVM + Manual DI)](#6-app-architecture-mvvm--manual-di)
7. [Data Layer](#7-data-layer)
8. [The AI Engine (Domain Layer)](#8-the-ai-engine-domain-layer)
9. [Voice Input](#9-voice-input)
10. [UI Layer — Screens, Navigation, Theme](#10-ui-layer--screens-navigation-theme)
11. [The Offline Knowledge Core (Subproject)](#11-the-offline-knowledge-core-subproject)
12. [Android ↔ Knowledge Core Contract](#12-android--knowledge-core-contract)
13. [Knowledge Search Quality Engineering (Recent Work)](#13-knowledge-search-quality-engineering-recent-work)
14. [Testing Strategy (100 Tests)](#14-testing-strategy-100-tests)
15. [Documentation Set](#15-documentation-set)
16. [Security, Privacy & Offline Guarantees](#16-security-privacy--offline-guarantees)
17. [Known Limitations, Stubs & Inconsistencies](#17-known-limitations-stubs--inconsistencies)
18. [Project History / Timeline](#18-project-history--timeline)
19. [Roadmap](#19-roadmap)
20. [Appendices](#20-appendices)

---

## 1. What This Project Is

**Kuzmix AI** is an **offline-first Android AI companion app** that answers questions, tutors students, and assists with code — **entirely without an internet connection**. It is not a thin client for a cloud LLM: the app declares **no `INTERNET` permission at all**, and every "AI" feature runs on-device.

The product combines three pillars:

1. **An Android app** (`com.kuzmix.ai`) — Kotlin + Jetpack Compose, single Gradle module `:app`. Chat UI, voice dictation, code assistant, grammar checker, profile/settings.
2. **An offline knowledge engine** — a locally rendered, template + retrieval engine (`KnowledgeEngine`) that classifies intent and answers from a bundled **40 MB SQLite knowledge corpus** (2,577 concepts, FTS5 full-text search) plus curated response templates. No network, no learned generative model, no hallucinating cloud.
3. **An offline knowledge core** (TypeScript/Python subproject) — the **authoring, build, validation, and browsing toolchain** that produces the SQLite corpus the app ships in its assets, plus an `ANDROID_QUERY_CONTRACT.md` that freezes the SQL interface between the two.

**Key facts at a glance:**

| Property | Value |
|---|---|
| Package / applicationId | `com.kuzmix.ai` |
| App version | `versionName 1.0.0`, `versionCode 1` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 |
| Permissions declared | `RECORD_AUDIO` only (no INTERNET) |
| Knowledge corpus | `kuzmix_knowledge.sqlite`, 40,189,952 B (38.33 MB), SHA-256 `9788BC84EDE20C412D7E4CDB5FB7C33C45E4F376F35A29094150F58F587E69E3` |
| Corpus contents | 9 domains · 61 categories · 122 topics · **2,577 concepts** · 4,408 knowledge records · 8,891 RAG chunks · 6,134 aliases · 2,587 misconceptions · 2,617 relationships · 7,031 FTS5 rows · 16 attributed sources |
| Unit tests | **100** (all passing) |
| Latest build | `build_apk13.bat` → `app-debug.apk` 26,052,399 bytes, `BUILD SUCCESSFUL`, `EXIT=0` |
| Creator | Oni Oluwatobi, under The Kreadive Galaxy |
| Tagline | "Smarter Conversations. Bigger Ideas. A Brighter You." · "Empowering the World with Innovative AI" |

---

## 2. Design Philosophy & Guarantees

The knowledge-core README states the philosophy, and the app code enforces it:

1. **Zero cloud.** No network permission exists in the manifest. All retrieval, rendering, and (future) model inference happen on the phone. The app works in airplane mode.
2. **Structured knowledge, not Q&A dumps.** The corpus stores *interconnected intellectual concepts*: summary definitions, pedagogical explanations, key properties/formulas, misconceptions with corrections, prerequisites, and a directed knowledge graph — not scraped trivia.
3. **Anti-hallucination guardrails.** Every concept carries explicit `misconceptions` (beliefs) with `corrections`; answers are grounded in retrieved corpus rows with confidence gates, and a fallback-loop protector prevents repeated failed answers.
4. **Mobile RAM discipline.** Read-only access (`PRAGMA query_only=ON`), memory-mapped I/O (256 MB `mmap_size`), bounded result sets (`LIMIT`, max 20 rows), pre-computed chunks with `token_estimate` (approx. chars/4) instead of on-device tokenizers, a bounded LRU concept cache (15–40 entries by device class), and a capped FTS row window (600 rows max).
5. **Graceful degradation (3 tiers).** SQLite knowledge core → Room `concepts` table seeded from JSON packs → canned response templates. Any tier can fail without killing the app.
6. **Honest stubs.** Features that would require a real generative model (image generation, translator, local model) **say so plainly in the UI** instead of faking output.
7. **Deterministic, testable answers.** 100 unit tests assert exact answer strings, intent routing, SQL behavior, and scoring — reproducibility over vibes.

---

## 3. Repository Structure

```
Kuzmix AI/                                 <- workspace root (not a git repo)
|
+-- app/                                   * THE ANDROID APP (Gradle module :app)
|   +-- build.gradle.kts                   app build config (69 lines)
|   +-- proguard-rules.pro
|   +-- src/
|       +-- main/
|       |   +-- AndroidManifest.xml        RECORD_AUDIO only; single MainActivity
|       |   +-- assets/
|       |   |   +-- kuzmix_knowledge.sqlite        * 40,189,952 B corpus
|       |   +-- java/com/kuzmix/ai/
|       |   |   +-- KuzmixApp.kt           Application -> AppContainer
|       |   |   +-- MainActivity.kt        theme + edge-to-edge + Compose tree
|       |   |   +-- di/AppContainer.kt     manual dependency container
|       |   |   +-- data/
|       |   |   |   +-- local/             Room: KuzmixDatabase, entities, DAOs
|       |   |   |   +-- preferences/       UserPreferences (DataStore)
|       |   |   |   +-- repository/        ChatRepository, SettingsRepository, AIRepository
|       |   |   |   +-- knowledge/         KnowledgeCoreStore, KnowledgeDataSource,
|       |   |   |                          KnowledgeSeeder, KnowledgePackParser, sources...
|       |   |   +-- domain/ai/             * engine: KnowledgeEngine, KnowledgeIndex,
|       |   |   |                          QueryNormalizer, FtsMatcher, IntentClassifier,
|       |   |   |                          ContextResolver, StudyEngine, DomainBridge,
|       |   |   |                          ConceptCache, LocalAIEngine (stub)...
|       |   |   +-- voice/                 SpeechInputController (on-device STT)
|       |   |   +-- tools/                 GrammarChecker
|       |   +-- ui/
|       |       +-- theme/                 Color.kt, Theme.kt, Type.kt, Shape.kt, Spacing.kt
|       |       +-- navigation/            Screen.kt, KuzmixNavGraph.kt
|       |       +-- components/            KuzmixButton, KuzmixChatBubble, TopBar/BottomBar...
|       |       +-- screens/               home, chat, chats, code, imagegen, profile,
|       |       |                          tools, welcome
|       |       +-- MainViewModel.kt       shared VM + KuzmixViewModelFactory
|       |       +-- KuzmixApp.kt           composable root
|       +-- res/                           raw JSON packs, drawables, strings, themes
|       +-- test/java/com/kuzmix/ai/       * 12 test files -> 100 tests
|
+-- docs/
|   +-- USER_MANUAL.md                     19,480 B -- 12-section end-user manual
|   +-- PROJECT_DOCUMENTATION.md           <- this file
|
+-- kuzmix-ai-<EM DASH>-offline-knowledge-core/   * KNOWLEDGE CORE SUBPROJECT
|   +-- server.ts                          Express API + Vite middleware
|   +-- src/App.tsx                        888-line React dashboard (5 tabs)
|   +-- package.json                       db:build / db:test / db:validate / db:export
|   +-- KUZMIX_KNOWLEDGE_README.md         main README (metrics, build commands)
|   +-- kuzmix_knowledge.sqlite            40,189,952 B (copy #1, identical SHA)
|   +-- kuzmix_knowledge.pre-improve.bak.sqlite   20,500,480 B (schema v1.0.0 snapshot)
|   +-- kuzmix-knowledge/                  * actual knowledge project
|       +-- kuzmix_knowledge.sqlite        40,189,952 B (copy #2, identical SHA)
|       +-- schema/                        001_initial_schema.sql, 002_fts_and_indexes.sql,
|       |                                  003_views_and_contracts.sql
|       +-- data/                          domains/categories/topics/sources/concepts
|       |                                  (11 files, 2,577 concepts)/rewrites (batch 001-004)
|       +-- scripts/                       build_sqlite.py (489 lines), apply_rewrites.py,
|       |                                  enrich_corpus.py, generators/ (9 files)
|       +-- tools/                         query_cli.ts, import_pack.ts, export_dataset.ts
|       +-- validation/validate_database.ts  9-point validation
|       +-- tests/test_database.ts         14 DB tests
|       +-- exports/kuzmix_knowledge_export.json   13,708,903 B
|       +-- docs/                          ANDROID_QUERY_CONTRACT.md, SCHEMA_ARCHITECTURE.md
|
+-- database/kuzmix-db.json                73 B empty stub ({...}) -- unused
+-- .reflex/                               Reflex tooling residue (unused)
|
+-- main.py / rag_engine.py / knowledge_base.py / requirements.txt / kuzmix_rag_db.pkl
|                                          * original Python prototype (not in build)
|
+-- build_apk.bat ... build_apk13.bat      13 build-script iterations
+-- run_tests.bat ... run_tests11.bat      11 test-script iterations
+-- apk1-out.log ... apk13-out.log (+ -err)  build logs
+-- test1-*.log ... test11-*.log           test logs
|
+-- settings.gradle.kts / build.gradle.kts / gradle.properties / gradlew(.bat)
+-- gradle/libs.versions.toml              version catalog
+-- local.properties                       sdk.dir (machine-local)
```

Notes:
- There is **no root `README.md`, no `.gitignore`, and no git repository** anywhere in the workspace.
- The knowledge-core folder name contains an **em dash (U+2014)**: `kuzmix-ai-—-offline-knowledge-core` — a recurring operational hazard for shell commands.
- The asset DB exists in **three byte-identical copies**: app asset + both knowledge-core copies (same SHA-256 above).

---

## 4. Technology Stack

### Android app

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.4.10 |
| UI | Jetpack Compose (BOM), Material 3, `material-icons-extended` | BOM 2026.08.00 |
| Navigation | `androidx.navigation:navigation-compose` | 2.9.0 |
| Build | Android Gradle Plugin | 9.1.0 |
| KSP (Room) | com.google.devtools.ksp | 2.3.6 |
| Gradle wrapper | Gradle | 9.3.1 |
| Persistence | Room (runtime, ktx, compiler) | 2.7.1 |
| Preferences | DataStore Preferences | 1.1.7 |
| Async | Kotlin coroutines | 1.10.2 |
| Lifecycle | runtime-ktx, viewmodel-compose, runtime-compose | 2.9.0 |
| Activity | activity-compose | 1.10.1 |
| Core | core-ktx | 1.16.0 |
| JSON | `org.json:json` (tests) / platform `org.json` (app) | 20240303 |
| Tests | JUnit 4 | 4.13.2 |
| Java | source/target 17 | — |

**Notably absent (by choice):** no Hilt/Koin (manual DI), no Retrofit/OkHttp (no network), no Robolectric/Mockito, no Media3, no accompanist, no `androidTest` source set at all.

`gradle.properties`: JVM heap 4096 m, parallel + caching enabled, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `android.suppressUnsupportedCompileSdk=37.0`.

### Knowledge core subproject

| Concern | Technology |
|---|---|
| Server | Express 4.21 + Vite 8.3 (dev middleware) + `node:sqlite` (`DatabaseSync`) via `tsx` 4.21 |
| Frontend | React 19.0.1 + Tailwind 4.3 + lucide-react + motion, single-file `App.tsx` |
| Database build | Python 3 (`sqlite3` stdlib) — `build_sqlite.py`, 489 lines |
| Database | SQLite (FTS5, `unicode61 remove_diacritics 2`), journal `delete` for shipping |
| Types | TypeScript 7.0.2 (`tsc --noEmit` = lint) |
| Original prototype | Python + `sentence-transformers` (all-MiniLM-L6-v2) — legacy only |

---

## 5. Build System & Release Process

### Scripts

Thirteen one-shot `.bat` files record the build iteration history (each self-contained, output redirected to a log):

| Scripts | Behavior |
|---|---|
| `build_apk.bat` – `build_apk4.bat` | `gradlew.bat assembleDebug --no-daemon --console=plain > apkN-out.log 2> apkN-err.log`, logs `EXITCODE=` |
| `build_apk5.bat` – `build_apk7.bat` | drops `--no-daemon --console=plain`, logs `EXIT=` |
| **`build_apk8.bat` – `build_apk13.bat`** | **current convention:** `gradlew.bat testDebugUnitTest assembleDebug > apkN-out.log 2> apkN-err.log` + `echo EXIT=%ERRORLEVEL%` — build **and** unit tests in one gate |
| `run_tests*.bat` (x11) | `gradlew.bat testDebugUnitTest` with log redirection |

Success marker: log tail `BUILD SUCCESSFUL … / EXIT=0`. APK output: **`app\build\outputs\apk\debug\app-debug.apk`** (release build type additionally has `isMinifyEnabled=true`, `isShrinkResources=true`).

### Operational hardening (developer environment)

- **Retry script:** `run_apk13_retry.ps1` (temp dir) wraps `build_apk13.bat` and retries up to 5x if Gradle dies with *"stop command received"* — a symptom observed when a **parallel coding session** runs `gradlew --stop`.
- **Watchdog:** `watchdog.ps1` takes a `jstack` dump if a build stalls with no output.
- **Detached execution:** long builds are launched via `Invoke-CimMethod -ClassName Win32_Process -MethodName Create` and polled through log files, because the interactive shell can die with `Unknown: ChildProcess.kill`.

### Verification ritual (before any release)

1. `testDebugUnitTest` → **100/100 passing**.
2. `assembleDebug` → `BUILD SUCCESSFUL`, `EXIT=0`.
3. Extract DB from APK → verify **SHA-256 = `9788BC84…69E3`** and size **40,189,952 B** (the bundled corpus must be byte-identical to the canonical build; `KnowledgeCoreStore.DB_VERSION` must be bumped in lockstep with any asset change).
4. Sideload `app-debug.apk` (26,052,399 bytes at last build); no Android device was attached via adb in recent sessions, so on-device verification (e.g. theme switching) falls to manual sideload.

---

## 6. App Architecture (MVVM + Manual DI)

### Pattern

Classic **MVVM + Repository**, with **hand-rolled dependency injection**:

```
KuzmixApp (Application)
   +-- AppContainer  -------------------- owns ALL singletons
          +-- KuzmixDatabase (Room) ---> DAOs
          +-- UserPreferences (DataStore)
          +-- KnowledgeCoreStore (SQLite asset) --+
          +-- ConceptCache (LRU)                  | concept-source chain
          +-- RoomConceptSource (fallback) <------+
          +-- KnowledgeEngine --> AIRepository --> (Home/Chat/Code VMs)
          +-- LocalAIEngine (stub)
          +-- ChatRepository
          +-- SettingsRepository
                 ^
MainActivity -----+  KuzmixViewModelFactory(container) --> ViewModels --> Compose screens
```

### Construction order (`AppContainer.kt`, 104 lines)

Property initializers run in declaration order, then an `init` block kicks off async knowledge initialization:

1. `database = KuzmixDatabase.get(context)` — Room singleton, DB file `kuzmix.db`.
2. `preferences = UserPreferences(context)` — DataStore `kuzmix_prefs`.
3. `knowledgeDataSource = KnowledgeDataSource(context)` — raw-JSON loader.
4. `conceptCache = ConceptCache(cacheCapacity(context))` — capacity **30** default; **15** low-RAM devices; **25** if `memoryClass <= 128`; else **40**.
5. `seedGate = CompletableDeferred<Unit>()`, `kbStore = CompletableDeferred<KnowledgeCoreStore?>()` — async readiness gates.
6. `appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
7. **`init`:** launches on `appScope` —
   - `force = preferences.getKnowledgeDbVersion() != KnowledgeCoreStore.DB_VERSION`
   - `KnowledgeCoreStore.open(appContext, force)`; on success persist `DB_VERSION` to DataStore; complete `kbStore` (`null` on any Throwable, logged as `KuzmixSeed`);
   - `seeder.seedIfNeeded()` (JSON → Room fallback seed; failure logged "json fallback seed failed");
   - `finally` completes `kbStore` + `seedGate` and logs `knowledge init finished in …ms`.
8. `roomConceptSource = RoomConceptSource(dao, cache, awaitReady = { seedGate.await() })`
9. `conceptSource = KnowledgeCoreConceptSource(store = { kbStore.await() }, cache, fallback = roomConceptSource)`
10. `knowledgeEngine = KnowledgeEngine(knowledgeDataSource.loadShell(), conceptSource)`
11. `localAIEngine = LocalAIEngine()` (stub)
12. Public: `aiRepository`, `chatRepository`, `settingsRepository`.

### Entry points

- **`KuzmixApp : Application`** — `container = AppContainer(this)` in `onCreate`.
- **`MainActivity`** — `enableEdgeToEdge()`; `setContent { KuzmixAppContent(container) }`. Collects `settingsRepository.appearance` (`"Dark"` / `"Light"` / anything-else = system) → computes `dark: Boolean` → `LaunchedEffect` re-applies `enableEdgeToEdge` with `SystemBarStyle.dark/light(transparent)` (receiver re-cast via `LocalContext.current as? ComponentActivity`) → `KuzmixTheme(darkTheme = dark) { … KuzmixNavGraph(container, factory, sharedViewModel) }`.

### ViewModel wiring (`KuzmixViewModelFactory` in `ui/MainViewModel.kt`)

| ViewModel | Dependencies |
|---|---|
| `MainViewModel` | ChatRepository, SettingsRepository — first-launch flag, shared conversation creation, pending initial message |
| `ChatViewModel` | ChatRepository, AIRepository |
| `ChatsViewModel` | ChatRepository |
| `HomeViewModel` | ChatRepository, AIRepository |
| `ProfileViewModel` | SettingsRepository, AIRepository |
| `ImageGenViewModel` | none (stub) |
| `CodeViewModel` | ChatRepository, AIRepository |
| anything else | `IllegalArgumentException("Unknown ViewModel: …")` |

---

## 7. Data Layer

### 7.1 Room (`data/local/`)

`KuzmixDatabase` — `version = 2`, `exportSchema = false`, file **`kuzmix.db`**, double-checked-locking singleton.

| Entity | Table | Key columns |
|---|---|---|
| `ConversationEntity` | `conversations` | `id` (Long, auto), `title`, `createdAt`, `updatedAt`, `isCodeMode: Boolean = false` |
| `MessageEntity` | `messages` | `id` (Long, auto), `conversationId` FK → conversations **ON DELETE CASCADE**, `role`, `content`, `timestamp`; index on `conversationId` |
| `ConceptEntity` | `concepts` | `id` (String PK), `type`, `domain`, `name`, `definition`, `aliasesCsv`, `keywordsCsv`, `relatedCsv`, `examplesCsv`, `misconceptionsCsv`, `formula`, `confidence` — list fields joined with the ASCII unit-separator control character (U+001F), chosen because example text contains commas |
| `ConceptFtsEntity` | `concepts_fts` | `@Fts4` mirror: `conceptId`, `name`, `definition`, `aliasesCsv` |

**`MIGRATION_1_2`** created the `concepts` table + `concepts_fts` (FTS4) — evidence of evolution from a chat-only v1 schema.

**DAOs:**
- `ConceptDao` — `searchFts(match, limit)` (`SELECT conceptId FROM concepts_fts WHERE concepts_fts MATCH :match LIMIT :limit`), `getByIds(ids)` (`IN (:ids)`), `getById`, `insertConcepts`/`insertFts` (REPLACE), `deleteAllConcepts/Fts`, `count`.
- `ConversationDao` — `observeAll(): Flow` (`ORDER BY updatedAt DESC`), `getAll`, `getById`, `insert → Long`, `update`, `touch(id)` (bumps `updatedAt`), `delete`.
- `MessageDao` — `observeForConversation` (`ORDER BY timestamp ASC`), `getForConversation`, `insert → Long`, `deleteForConversation`.

### 7.2 UserPreferences (DataStore `kuzmix_prefs`)

| Key object | Pref key | Type | Default |
|---|---|---|---|
| `FIRST_LAUNCH` | `first_launch_done` | Boolean | `false` |
| `USER_NAME` | `user_name` | String | `"Oni Oluwatobi"` |
| `USER_EMAIL` | `user_email` | String | `thekreadivegalaxy@gmail.com` |
| `APPEARANCE` | `appearance` | String | `"Dark"` |
| `LANGUAGE` | `language` | String | `"English"` |
| `NOTIFICATIONS_ENABLED` | `notifications_enabled` | Boolean | `true` |
| `SEED_VERSION` | `knowledge_seed_version` | Int | `0` |
| `KNOWLEDGE_DB_VERSION` | `knowledge_db_version` | Int | `0` |

Exposed as `Flow`s (`appearance`, `userName`, …) plus suspend setters. `knowledge_db_version` drives asset re-copy; `knowledge_seed_version` drives JSON re-seed (`KnowledgeSeeder.SEED_VERSION = 1`).

### 7.3 KnowledgeCoreStore — the SQLite asset layer (`data/knowledge/KnowledgeCoreStore.kt`, 284 lines)

Constants: `DB_VERSION = 4` (*"Bump together with the bundled asset to force a re-copy"*), `ASSET_NAME = "kuzmix_knowledge.sqlite"`, `MAX_ROW_WINDOW = 600`.

- **`open(context, forceCopy)`** — copies `assets/kuzmix_knowledge.sqlite` → `filesDir/knowledge/` via temp-file + atomic rename (forced when version changed or missing); opens `SQLiteDatabase.openDatabase(…, OPEN_READWRITE)`; applies `KnowledgePragmas.FIRST_OPEN` through value-returning helpers (`execPragma` — `rawQuery(...).use { moveToFirst }` for values, `execSQL` for setters); **probes FTS5** with `SELECT 1 FROM knowledge_fts WHERE knowledge_fts MATCH 'kuzmix' LIMIT 1`; returns `null` (→ fallback) on any failure. `query_only=ON` is applied at the PRAGMA level, so writes are blocked even though the file is opened read-write.
- **`searchIds(match, limit)`** — the heart of search (contract Query D, see section 12):

```sql
SELECT concept_id FROM (
  SELECT concept_id, bm25(knowledge_fts, 0, 0, 10.0, 1.0, 8.0, 1.0, 1.0, 4.0) AS r
  FROM knowledge_fts WHERE knowledge_fts MATCH ?
  ORDER BY r ASC LIMIT ?
) GROUP BY concept_id ORDER BY MIN(r) ASC LIMIT ?;
-- args: match, window, limit   where window = (limit*12).coerceAtMost(600)
```

  Weighted BM25: **title x10, aliases x8, keywords x4, content/category/domain x1, concept_id/record_id x0**. The subquery exists because SQLite rejects `MIN(bm25(...))` directly (*"unable to use function bm25 in the requested context"*); row-level `LIMIT` alone starved recall (probe: `"water"` → 39 vs 50 unique concepts with the windowed form).
- **`lookupExact(normalized)`** — contract Query B: `SELECT id FROM concepts WHERE canonical_name_normalized = ? UNION SELECT concept_id FROM aliases WHERE alias_normalized = ? LIMIT 1`.
- **`rowsByIds(ids)`** — five grouped reads preserving input order: base concepts (JOIN `domains`, JOIN `categories`, LEFT JOIN `topics`), `aliases`, `relationships` (`ORDER BY weight DESC`), `misconceptions`, `knowledge_records` with formula precedence rank `formula=0, law=1, theorem=2` → `CoreRow` (16 fields).
- Also `rowById(id)`, `close()`.

**Pragma policy (`KnowledgePragmas`):** `RETURNS_ROWS = {journal_mode, mmap_size}`; `FIRST_OPEN = [journal_mode=DELETE, query_only=ON, temp_store=MEMORY, mmap_size=268435456]`.

### 7.4 Concept-source chain (graceful degradation)

```
KnowledgeCoreConceptSource(store, cache, fallback=RoomConceptSource)
        |  store == null? ----------------> RoomConceptSource (FTS4 Room table, seeded from JSON)
        |  cache hit?  -------------------> ConceptCache (bounded LRU)
        +-- else: SQLite 4-tier candidates -> hydrate (only cache misses) -> mapper -> cache
```

**`KnowledgeCoreConceptSource`** — `CANDIDATE_LIMIT = 50`, `LOOKUP_LIMIT = 25`, runs on `Dispatchers.IO`.

**`candidateIds(s, phraseText, tokens, limit)` — 4-tier cascade**, each tier fetching the *full* limit and accumulating into a `LinkedHashSet` (dedup + first-seen order preserved):

| Tier | Condition to run | Query |
|---|---|---|
| 1. Phrase | always | `FtsMatcher.phraseMatch(phraseText)` — raw adjacency, **stopwords kept** (so `"laws of motion"` matches) |
| 2. OR tokens | `size < limit` | `FtsMatcher.buildMatch(contentTokens)` — `"a" OR "b" …` |
| 3. Inflection | `size < limit/2` | expand each token via `inflectionVariants` → OR-match |
| 4. Prefix | `size < limit/2` | `prefixMatch(tokens)` → `"mito"*` |

Result: `found.toList().take(limit)`.

- **`lookup(entity)`** — exact normalized, then exact stripped; on miss, fuzzy via `candidateIds(LOOKUP_LIMIT)` + `LooseEntityMatcher.firstMatch`.
- **`byId(id)`** — cache-first, then `rowById` → mapper → cache.

**`RoomConceptSource`** — fallback with the same limits; two-tier candidates (phrase tier first for multi-word aliases, then OR fill); awaits `seedGate` before touching FTS.

**`ConceptCache`** — synchronized `LinkedHashMap(capacity, 0.75f, accessOrder = true)` LRU with `removeEldestEntry = size > capacity`; `get / contains / put / getOrPut / clear`.

**`LooseEntityMatcher`** — resolution ladder: exact stripped name → exact alias → token containment (all key tokens in entity tokens AND `keyTokens.size >= entityTokens.size - 1`) → single-token word containment (name, then aliases).

**`KnowledgeCoreMapper`** — `CoreRow → ConceptRecord`; caps `MAX_EXAMPLES=3`, `MAX_RELATED=8`, `MAX_MISCONCEPTIONS=4`, `MAX_KEYWORDS=40`, `MAX_RECORD_TITLES=5`; definition fallback `summaryDefinition.ifBlank { primaryExplanation }`; misconceptions formatted `"belief → correction"`; `formula()` preference: key-properties object whose `property` matches `(?i)formula|equation` → else record after last `:` containing `=`/`∝`/`^` → else first clause <=120 chars with an equation marker; confidence label `>=0.85 HIGH / >=0.60 MEDIUM / else LOW`.

### 7.5 JSON packs (secondary source)

| File | Size | Content |
|---|---|---|
| `res/raw/knowledge_base.json` | 19,397 B | canned `responses` + `variations` (always-resident shell, ~40 KB merged via `loadShell()`) |
| `res/raw/kp_subjects.json` | 21,340 B | subjects (roadmaps, detect keywords), formulas, misconceptions, relations; default `knowledgeVersion "6.0"` |
| `res/raw/kp_concepts_science.json` | 318,762 B | concepts (one-time seed only) |
| `res/raw/kp_concepts_tech.json` | 300,957 B | concepts |
| `res/raw/kp_concepts_humanities.json` | 161,585 B | concepts |

- **`KnowledgePackParser`** (org.json) — `parseBase` → responses/variations; `parseSubjects` → subjects/formulas/misconceptions/relations; `parseConcepts` → `List<ConceptRecord>` (defaults: `type=CONCEPT`, `formula=""`, `confidence="HIGH"`); `merge(...)` → `KnowledgePack`.
- **`KnowledgeDataSource`** — `loadShell()` (base + subjects, no concepts — resident), `loadConcepts()` (3 concept files — seed only), `loadAll()`.
- **`KnowledgeSeeder`** — `SEED_VERSION = 1`, `INSERT_CHUNK = 250`; `seedIfNeeded()` skips if DataStore version >= SEED_VERSION; full-replace seed (delete FTS + concepts, chunked inserts). Requires no network.

---

## 8. The AI Engine (Domain Layer)

### 8.1 What "AI" means here

There is **no learned model and no network call** in the shipped app. Answers come from:

1. **Intent classification** (regex/heuristic, 27 intents),
2. **Retrieval** from the SQLite corpus or JSON pack (weighted BM25 + hand-tuned scoring),
3. **Deterministic rendering** (Kotlin string templates + curated JSON responses),
4. **Deterministic calculators** (percentage regex + recursive-descent arithmetic evaluator).

`LocalAIEngine` is a deliberate **stub** for a future on-device model (`isAvailable = false`, `modelStatus: NOT_INSTALLED/LOADING/READY/ERROR`, KDoc: *"llama.cpp / GGUF / ONNX / LiteRT … NO network / remote API is used or will be used"*). `AIRepository.ask()` tries it first when available, then always falls back to `KnowledgeEngine`.

### 8.2 KnowledgeEngine pipeline (628 lines)

`engineName = "Kuzmix Knowledge Base"`, `isAvailable = true`, `isLocal = true`.

```
generateResponse(query, history)                  [Dispatchers.Default]
 1. QueryNormalizer.normalize(query)              -> NormalizedQuery(raw, normalized, tokens)
 2. IntentClassifier.classify(nq)                 -> IntentMatch(intent, entity, entity2)
 3. ContextResolver.resolve(history)              -> ConversationContext (active subject/topic, education level)
    resolver.applyQuery(ctx, query)               -> detects subject/level from the new query
 4. if intent == DEFINITION -> re-resolve:
       index.resolveEntity(entity)?.kind == SUBJECT -> SUBJECT_DEFINITION
                                        else        -> CONCEPT_DEFINITION
 5. big `when (intent)` -> one of the response builders (below)
 6. record lastTrace (EngineTrace: rawQuery, normalizedQuery, intent, entity,
       activeSubject/Topic, candidateId, score, confidence, provider="LOCAL",
       result = success|fallback)
```

**Intent routing table (`when(intent)` → builder):**

| Intent(s) | Builder / behavior |
|---|---|
| CODE_GENERATION / CODE_EXPLANATION / DEBUGGING / FEATURE_REQUEST / PROJECT_GENERATION / CODE_ASSISTANT | `codeResponse` — 5 canned code-intent replies |
| CAPABILITY_QUERY / SUBJECT_CAPABILITY | `capabilityResponse` (lists subjects) |
| STUDY_ROADMAP | `roadmapResponse` (via `StudyEngine`: first 3 roadmap items = foundations, next 4 = later) |
| STUDY_HELP / STUDY_START | `studyHelpResponse` / `StudyEngine.startResponse` |
| DEFINITION family | `definitionResponse` (see thresholds) |
| COMPARISON | `comparisonResponse` (renders both entities + differences) |
| CALCULATION | `calculationResponse` — `% of` regex `(\d+(?:\.\d+)?)\s*%\s*of\s*(\d+(?:\.\d+)?)` + `ExprEval` recursive-descent evaluator (`+ - * / ^ ( )`, maps `×` `÷`) |
| EXAMPLE_REQUEST | `exampleResponse` |
| PRACTICE_QUESTION | `practiceResponse` |
| IDENTITY | `identityResponse` (mentions Kuzmix AI, Oni Oluwatobi, offline) |
| ABOUT_KUZMIX / developer / provider | `aboutResponse` / `developerResponse` / `providerResponse` |
| GREETING / FAREWELL / THANKS | curated `pack.responses` strings |
| HELP | `helpResponse` (lists capabilities) |
| SMALL_TALK with entity `__continue__` | `study.continueWithSubject` |
| GENERAL_KNOWLEDGE / UNKNOWN | `retrievalResponse ?: legacyLookup ?: fallbackFor` |

**Answer acceptance thresholds:**

| Path | minScore | Accept when | Notes |
|---|---|---|---|
| `definitionResponse` primary | 0.20 | `best.score >= 0.28` | exact entity bypasses at score 1.0/HIGH |
| `definitionResponse` broad retry | 0.15 | `>= 0.30` | prefixed *"Closest match in my knowledge:"* |
| `retrievalResponse` | 0.20 | `score >= 0.30` | exact entity → 1.0/HIGH |
| `searchPreview` (home search) | 0.15 | — | *"Lower minScore than answer retrieval … engine still gates actual answers at 0.20+"* |

**Fallback loop protection:** `FALLBACK_A = "I couldn't find enough information for that exact request"`, `FALLBACK_B = "Let me try from another angle"` (used when `ctx.lastAssistantWasFallback`), both followed by guidance text. Fallback detection feeds `lastTrace.result`.

**`legacyLookup`** (template tier): exact `pack.variations[cleaned]` → responses; substring trigger matching (either side ≥4 chars); direct `pack.responses[cleaned]`.

**Renderers:** `renderSubject` (definition + up to 6 subtopics + roadmap teaser), `renderConcept` (definition, `Formula: …`, first example, up to 3 related), `renderFormula` (`name: formula`, `Where k = v`, explanation).

### 8.3 IntentClassifier (378 lines, 27 intents)

`Intent` enum: GREETING, FAREWELL, THANKS, SMALL_TALK, HELP, CAPABILITY_QUERY, SUBJECT_CAPABILITY, STUDY_HELP, STUDY_START, STUDY_ROADMAP, DEFINITION, SUBJECT_DEFINITION, CONCEPT_DEFINITION, EXAMPLE_REQUEST, COMPARISON, PRACTICE_QUESTION, CALCULATION, CODE_ASSISTANT, CODE_GENERATION, CODE_EXPLANATION, DEBUGGING, FEATURE_REQUEST, PROJECT_GENERATION, IDENTITY, ABOUT_KUZMIX, DEVELOPER_INFO, GENERAL_KNOWLEDGE, UNKNOWN.

**Classification order (first match wins):**
1. Exact code quick-prompts ("create a simple android app", "fix this error in my code", "explain this code", "add a new feature", "generate a full project") → sub-intent (DEBUGGING / CODE_EXPLANATION / FEATURE_REQUEST / PROJECT_GENERATION / CODE_GENERATION / CODE_ASSISTANT).
2. 15 `codePatterns` regexes.
3. Capability patterns (`SUBJECT_CAPABILITY` if pattern contains "subjects").
4. Identity → IDENTITY; developer patterns (incl. `kreadive`, `oni oluwatobi`) → DEVELOPER_INFO; aboutKuzmix; provider patterns → ABOUT_KUZMIX (entity="provider").
5. `"what do you know|can you tell me|tell me about|on X"` → DEFINITION (later re-resolved to subject vs concept).
6. Roadmap → STUDY_ROADMAP; study-help → STUDY_HELP (+ `extractSubjectEntity`, entity capped at 60 chars); `^(let's|we should|i should) (study|learn|start|begin)` → STUDY_START.
7. `continuePatterns` full-match → SMALL_TALK with entity `__continue__`.
8. Calculation patterns → CALCULATION.
9. Comparison regexes capture `entity` + `entity2`.
10. Definition patterns (capture entity, fallback `text.removePrefix(match)`), example, practice patterns.
11. Greeting/farewell/thanks/help **full-match** regexes — including **Nigerian-English** forms ("how far na", "how e dey").
12. Small-talk full-match.
13. Text starting what/how/why/when/where/who → `GENERAL_KNOWLEDGE(cleanEntity(text))`.
14. Else → `UNKNOWN(cleanEntity(text))`.

`cleanEntity` strips leading filler (`^(the|a|an|is|are|about|of|on|in|for)\s+`) repeatedly.

### 8.4 QueryNormalizer (111 lines)

- **Lowercase/trim**; strip leading code context `^language\s*:\s*[^.]*\.\s*` (used by Code Assistant's `"Language: {lang}. {text}"` prefix); curly→straight quotes; **~28 contraction expansions** ("what's"→"what is", "can't"→"cannot") and **~18 slang expansions** (u→you, pls→please, **wetin/watin→what, sabi→know, fit→can, abeg→please**, gonna/wanna/gotta) with `(?<=\s|^)…(?=\s|$|[?!.])` boundaries; `o{3,}`→"o"; `×→*`, `÷→/`, minus/en-dash→`-`; strip chars outside `[^\w\s*+/^().%-]`; collapse whitespace; trim edge `[\s?.,!]+`; tokenize on whitespace and trim `?.!,;:"'()`.
- **`stopwords`** — 96-word set (a…why) **plus "kuzmix", "assistant", "ai"**.
- **`contentTokens(tokens)`** — keep len > 1, non-stopword, non-all-digits.
- **`phraseText(nq)`** — strips **leading interrogative** (`what/who/where/when/why/how/which…`, `define`, `explain`, `describe`, `tell me about`, `what do you mean by`, `difference between`, `compare`, `list`, `give me`, `show me`, `name`) and **leading auxiliary** (`^(is|are|was|were|do|does|did|can|could|will|would|should)(?:\s|$)`), trims edge punctuation — **keeps all remaining words including stopwords**. Rationale: `"laws of motion"` must survive (probe: 1 hit vs 0 before).
- **`stripPunct(text)`** — lowercase, non-word→space, collapse.

### 8.5 FtsMatcher (77 lines)

- `MAX_TERMS = 12`.
- **`buildMatch(tokens)`** — dedupe → take 12 → each wrapped in double quotes → join `" OR "` (quoting neutralizes FTS operator injection; embedded quotes stripped first). E.g. `"cell" OR "wall"`.
- **`phraseMatch(text)`** — collapse whitespace → `"$clean"` (FTS adjacency phrase).
- **`prefixMatch(tokens)`** — tokens **length ≥ 3** only → `"$it"*`, OR-joined (e.g. `"mito"*`).
- **`inflectionVariants(token)`** — empty if `len < 3` **or ends with `-is`, `-us`, `-ss`** (Latin/Greek endings skipped — `mitosis` → none). Rules: `ies`(len>4)→`y`; `es` after `[sxz]es|[csz]hes`(len>4)→drop 2; trailing `s`→drop 1; consonant+`y`(len>3)→`ies`; `[sxz]$|[csz]h$`→+`es`,+`s`; else +`s`. Results filtered to ≥3 chars, never equal to input.

### 8.6 KnowledgeIndex — scoring (271 lines)

Builds entries: subjects (Kind.SUBJECT), pack concepts (Kind.CONCEPT), formulas (Kind.FORMULA, alias = the formula string); `byName`/`byAlias` maps of stripped-normalized keys.

**`resolveEntity`:** byName → byAlias → (legacy only) token-subset fuzzy / word containment → with source wired, falls through to `source.lookup(entity)`.

**`search(nq, context, intent, minScore = 0.20)`:**

1. **Candidate gathering.** Legacy mode = all static entries. Disk mode = static entries + `source.candidates(nq)` mapped to entries, **deduplicated by id** (static slot kept via `seen` set; dynamic-only ids appended).
2. **FTS rank prior:** `ftsRank[id] = 1.0 − i/dynamic.size` (i = position in FTS result order), `putIfAbsent`.
3. **Per-entry flags:** `nameHit` (name token ∈ qTokens), `aliasHit` (word-boundary regex `(?<![a-z0-9])escaped(?![a-z0-9])` vs normalized text), `keywordHit`, `definitionHit` → **skip entry entirely if all four false**.
4. `semantic = |qTokens ∩ (definitionTokens ∪ keywordTokens ∪ nameTokens)| / |qTokens|`
5. **Coverage gate:** `if (semantic < 0.33 && !nameHit && !aliasHit) continue` — drops entries covering less than 1/3 of query tokens (single-token queries can't trip it).
6. `lexical = |qTokens ∩ (nameTokens ∪ keywordTokens)| / |qTokens|`
7. `aliasScore` = 1.0 on aliasHit or contained normalizedName, else 0.
8. `contextScore`: active-subject domain match (via `DomainBridge.matches`) → 1.0; subtopic mention → 0.6; else 0.
9. `intentScore` by intent/kind (e.g. SUBJECT_DEFINITION: subject 1.0 / other 0.35; DEFINITION family: concept 1.0, subject 0.85, formula 0.25; CALCULATION: formula 1.0 / 0.4; study intents: subject 1.0 / 0.3; COMPARISON 0.7; GENERAL/UNKNOWN 0.5).
10. **Final weighted score:**

```
score = lexical×0.24 + alias×0.18 + semantic×0.28 + context×0.14 + intent×0.10 + fts×0.06
```

Keep if `score ≥ minScore`; sort descending.

**Confidence:** exact → HIGH; `≥0.55` → HIGH; `≥0.32` → MEDIUM; else LOW.

### 8.7 Supporting domain classes

- **`ConversationContext`** — `activeSubject, activeSubjectName, activeTopic, activeMode, lastIntent, educationLevel, lastAssistantWasFallback`.
- **`ContextResolver`** — study/code marker lists; education-level regex `\b(ss[1-3]|jss[1-3]|ss[1-9]|waec|neco|gce|grade\s*\d|form\s*\d|class\s*\d)\b`; `detectSubject` = word-boundary name/alias match else ≥2 `detectKeywords` hits; defines `FALLBACK_A/B`.
- **`DomainBridge`** — 9 domain slugs (`sci_math`, `eng_tech`, `computing_ai`, `business_econ`, `humanities`, `society_culture`, `health_life`, `creative_arts`, `practical_life`) ↔ 13 subject ids (biology→{health_life, sci_math}; general_knowledge→all; …).
- **`StudyEngine`** — `startResponse`, `roadmapResponse`, `genericStartResponse`, `continueWithSubject`.
- **Templates** — there are **no prompt-template resource files**; templates are inline Kotlin string literals + the JSON `responses`/`variations`.

### 8.8 AIRepository — chat flow

- `activeEngineName` = local if available else knowledge engine; `isOfflineReady`; `modelStatus`.
- **`ask(query, history)`** — try `localAIEngine` first (skip if unavailable/blank), else `knowledgeEngine.generateResponse`.
- **`searchKnowledge(query, limit=5)`** → `knowledgeEngine.searchPreview` (used only by Home search).
- **Chat persistence:** `ChatViewModel.sendMessage` inserts the user message, auto-renames `"New Chat"` → `text.take(40)`, calls `ask(text, history)` on `Dispatchers.Default` (history = full prior message list kept in sync from `observeMessages`), inserts the assistant reply.

### 8.9 Home knowledge search (`searchPreview`)

```kotlin
KnowledgeHit(id, name, summary, score)          // summary = definition.take(160)
searchPreview(query, limit = 5): List<KnowledgeHit>   [Dispatchers.Default]
  empty when no content tokens
  index.search(nq, ConversationContext(), GENERAL_KNOWLEDGE, minScore = 0.15)
  .take(limit)
```

`HomeViewModel`: `_searchQuery: MutableStateFlow("")` → `.debounce(250L).mapLatest { q -> if (q.trim().length < 2) emptyList() else aiRepository.searchKnowledge(q, 5) }` → `stateIn(WhileSubscribed(5000))` → `knowledgeHits` StateFlow (`@OptIn(FlowPreview, ExperimentalCoroutinesApi)`).

---

## 9. Voice Input

`voice/SpeechInputController.kt` (318 lines) — **on-device speech recognition only, two tiers:**

1. API 31+: `SpeechRecognizer.createOnDeviceSpeechRecognizer` (gated by `isOnDeviceRecognitionAvailable`).
2. Else: `SpeechRecognizer.createSpeechRecognizer` with `EXTRA_PREFER_OFFLINE = true`, `LANGUAGE_MODEL_FREE_FORM`, device locale, `EXTRA_MAX_RESULTS = 1`.

- State machine: `Idle / Listening / Processing / Unavailable / PermissionDenied(permanently) / Error`.
- `RECOGNITION_TIMEOUT_MS = 20_000`; `OFFLINE_UNAVAILABLE_MESSAGE = "Offline voice input isn't available on this device."`.
- Error mapping: NETWORK/SERVER → *"Offline speech recognition failed…"*; LANGUAGE_* → *"Offline speech for this language isn't installed…"*.
- `mergeDictation(existing, dictation)` — fills empty / appends / trims / leaves blank untouched (4 unit tests).
- Permission: `ChatScreen` requests `RECORD_AUDIO` via `rememberLauncherForActivityResult(RequestPermission())`, shows rationale on permanent denial; lifecycle `ON_PAUSE → stop()`, `onDispose → release()`.
- No Media3 anywhere.

`tools/GrammarChecker.kt` — regex rules: repeated words, double spaces, space-before-punctuation, missing space after punctuation, lowercase "i", sentence/text capitalization, missing ending punctuation; `check() → List<GrammarIssue(index, message, suggestion)>`, `correct()` applies fixes + appends `"."` if needed (10 tests; decimals deliberately not flagged).

---

## 10. UI Layer — Screens, Navigation, Theme

### Screens

| Screen | File | What it does |
|---|---|---|
| **Welcome** | `welcome/WelcomeScreen.kt` (124 L) | Logo, "Kuzmix AI", "by The Kreadive Galaxy", tagline "Smarter Conversations. Bigger Ideas." + orange "A Brighter You."; Get Started / Log In (both → `onGetStarted`) |
| **Home** | `home/HomeScreen.kt` (513 L) | Greeting "Good morning, {userName}" + "Thrive. Create. Build. Let's make it happen."; **search bar** (local state, filters recent chats by title/preview **and** feeds VM → Knowledge hit list via `KnowledgeHitItem`); 4 `QuickActionCard`s (New Chat, Image Generator, Code Assistant, Tools/Study plan); recent chats; `OfflineBadge` |
| **Chat** | `chat/ChatScreen.kt` (412 L) | LazyColumn of `KuzmixChatBubble`, composer (attach/mic/send), delete-conversation menu, snackbar, auto-scroll, `LaunchedEffect(conversationId) { loadConversation(id, initialMessage) }`, speech wiring + permission, lifecycle stop/release |
| **Chats** | `chats/ChatsScreen.kt` | Conversation list (`ChatListItem`), open/new/delete |
| **Code Assistant** | `code/CodeAssistantScreen.kt` (13,140 B) | Language chips (default "Android (Kotlin)"), `CodeTab`s, quick prompts; VM creates Room conversation titled "Code Assistant" `isCodeMode=true`; `sendPrompt` prefixes `"Language: {lang}. {text}"` (round-trips through `stripLeadingCodeContext`) |
| **Image Gen** | `imagegen/ImageGenScreen.kt` (11,385 B) | Prompt (≤500 chars)/style/aspect UI; `generate()` honestly answers *"Local image model not installed. Import a model in Settings → AI Model to generate images offline."* |
| **Profile** | `profile/ProfileScreen.kt` (480 L) | Avatar initial, "Free Plan", email; hard-coded stats (Projects 12, Chats 48, Tools 15); AI-model status card; Settings rows: **Appearance (Dark/Light/System → applied immediately)**, Language, Notifications, Privacy, About; Log Out → `resetFirstLaunch()` → Welcome |
| **Tools** | `tools/ToolsScreen.kt` (272 L) | Image Generator, Code Assistant, **Summarizer** (creates Chat conversation titled "Summarizer"), Translator (*"requires a local language model. Coming soon."*), **Grammar Check** (`GrammarCheckDialog` + copy-to-clipboard), More Tools (*"on the way"*) |

### Navigation (`ui/navigation/`)

- **Routes** (`Screen.kt`): `welcome`, `home`, `chats`, `tools`, `profile`, `chat/{conversationId}` (`Screen.Chat.create(id)`), `image_gen`, `code_assistant`. Bottom bar: 5 items with center `BottomNavItem("__create__")` (orange 44.dp `Add`).
- **`KuzmixNavGraph.kt` (234 L)** — first-launch gate: `isFirstLaunchDone == null` → spinner; `false` → Welcome (`markFirstLaunchDone()`); `true` → `Scaffold(bottomBar = KuzmixBottomBar)` shown **only** for routes `home|chats|tools|profile`; `NavHost(startDestination = "home")`; tab switches use `popUpTo("home"){saveState}` + `launchSingleTop` + `restoreState`; center + → `createConversation { navigate(chat) }`.
- **Home → Chat hand-off:** `onAskKnowledge = { q -> mainViewModel.setPendingInitialMessage(q); mainViewModel.createConversation(title = q.take(60)) { id -> navigate(Screen.Chat.create(id)) } }`; in the chat composable `remember { mainViewModel.consumePendingInitialMessage() }` → `ChatScreen(initialMessage=)`; `MainViewModel.pendingInitialMessage` is a plain `private var`, **one-shot** (nulled on read). `ChatViewModel.loadConversation(id, initialMessage)` sends it after state/observer setup.
- Other hand-offs: `onStudyPlan` → conversation titled "Study Plan"; Tools' Summarizer → titled "Summarizer"; Profile logout → `resetFirstLaunch()`.

### Theme system (rewritten — dark/light/system)

**`ui/theme/Color.kt` (95 L):**

- **Brand (static, both themes):** `KuzmixOrange #FF8C00`, `KuzmixOrangeDeep #FF6D00`, `KuzmixYellow #FFC107`, `KuzmixYellowSoft #FFD54F`, `KuzmixOrangeContainer 0x33FF8C00`, `KuzmixUserBubble #FF8C00`.
- **`data class KuzmixColors`** — 13 semantic slots: `background, surface, surfaceElevated, surfaceVariant, border, borderSubtle, textPrimary, textSecondary, textTertiary, textOnAccent, error, success, assistantBubble`.
- **`KuzmixDarkColors` / `KuzmixLightColors`** internal vals + `LocalKuzmixColors = staticCompositionLocalOf { KuzmixDarkColors }`; the 13 constants became `@Composable @ReadOnlyComposable get()` accessors (`KuzmixBackground`, `KuzmixSurface`, … `KuzmixAssistantBubble`) — **288 call-site references migrated, zero stragglers.**

| Slot | Dark | Light |
|---|---|---|
| background | `#0A0A0F` | `#F6F7FA` |
| surface | `#14141C` | `#FFFFFF` |
| surfaceElevated | `#1C1C26` | `#FFFFFF` |
| surfaceVariant | `#1E1E2A` | `#EDEFF4` |
| border | `#2A2A36` | `#D9DCE4` |
| borderSubtle | `#22222E` | `#E8EAF0` |
| textPrimary | `#FFFFFF` | `#12141A` |
| textSecondary | `#9CA3AF` | `#565D6B` |
| textTertiary | `#6B7280` | `#7C8391` |
| textOnAccent | `#0A0A0F` | `#0A0A0F` |
| error | `#F87171` | `#DC2626` |
| success | `#34D399` | `#0E9F6E` |
| assistantBubble | `#1A1A24` | `#EDEFF4` |

**`ui/theme/Theme.kt` (83 L):** `buildDarkScheme()` / `buildLightScheme()` map the semantic colors into Material3 `darkColorScheme` / `lightColorScheme` (secondaryContainer `0x2EFFC107` dark / `0x33FFC107` light; onError Black/White). `KuzmixTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` = `CompositionLocalProvider(LocalKuzmixColors provides …) + MaterialTheme(colorScheme, KuzmixTypography, KuzmixShapes)`.

**Wiring:** `MainActivity` collects `appearance` (Dark/Light/System) → `dark` → `LaunchedEffect` re-applies edge-to-edge system-bar styles → passes `darkTheme = dark` into `KuzmixTheme`. **Behavior: the choice applies immediately and is persisted for future launches** (Profile caption states exactly this).

**Also:** `Type.kt` (all SansSerif; displayLarge 40/48 Bold -0.5 … labelSmall 11/16 Medium +0.4), `Shape.kt` (6/10/14/18/24 dp + `Pill = 50.dp`), `Spacing.kt` (Xxs 2 … Huge 40, ScreenHorizontal 24, SectionGap 28, ComponentGap 16).

### Components (`ui/components/`)

`KuzmixButton` (+style enum), `KuzmixCard`, `KuzmixChatBubble`, `KuzmixTextField`, `KuzmixToolItem`, `KuzmixTopBar`, `KuzmixBottomBar` (NavigationBar, 5 specs, orange Add, transparent indicator), `OfflineBadge` (default text "Local AI", 7.dp orange dot), `KuzmixSectionHeader`.

---

## 11. The Offline Knowledge Core (Subproject)

Folder: `kuzmix-ai-—-offline-knowledge-core/` (name contains em dash **U+2014**). Its stated purpose (`KUZMIX_KNOWLEDGE_README.md`, 192 lines): the *"official offline knowledge engine for Kuzmix AI"* — a *"production-grade, structured, queryable SQLite knowledge database designed specifically to run offline on low-RAM mobile devices … and power local Retrieval-Augmented Generation."*

### 11.1 What it contains

- **Web shell** — Express + Vite + React dashboard for browsing/validating the corpus (see below).
- **`kuzmix-knowledge/`** — the real project: schema SQL, curated data, Python builder, validators, tests, export, contract docs.
- **Two byte-identical production SQLite copies** (40,189,952 B) + `pre-improve.bak` snapshot (20,500,480 B, schema 1.0.0).
- `metadata.json` — project "Kuzmix AI — Offline Knowledge Core", capability `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` (AI-Studio template residue; **the server uses no Gemini API**).

### 11.2 Web shell (`server.ts`, 185 lines)

Express on `PORT || 3000`, binds `0.0.0.0`; mounts Vite in middleware mode (dev) or serves `dist/` (prod). DB opened via `node:sqlite` `DatabaseSync` through `KuzmixKnowledgeClient` (`tools/query_cli.ts`).

| Endpoint | Purpose |
|---|---|
| `GET /api/stats` | file size + COUNT for every table + `PRAGMA integrity_check` |
| `GET /api/domains` | domains + category/concept counts |
| `GET /api/concepts?domain=&category=` | filtered concept list, joined names, `ORDER BY c.name` |
| `GET /api/concept/:id` | full hydration (aliases, misconceptions, records, chunks, relationships, comparisons, procedures, disambiguations); 404 if missing |
| `GET /api/search?q=` | FTS5 BM25 top-15 with `<b>` snippets |
| `GET /api/rag?q=&tokens=` | RAG context assembly (default 400 tokens) |
| `GET /api/validate` | runs `validateDatabase()` → report |
| `POST /api/run-tests` | runs the 14-test suite → *"All 14 automated tests passed cleanly"* |
| `GET /api/download/sqlite` | downloads the DB file |

**`package.json` scripts:** `dev`/`start` → `tsx server.ts`; `build` → `vite build`; `lint` → `tsc --noEmit`; **`db:build` → `tsx kuzmix-knowledge/scripts/build_database.ts`; `db:test`; `db:validate`; `db:export`.**

**React dashboard (`src/App.tsx`, 888 lines) — 5 tabs:**
1. **Overview** — live stats cards + domain grid.
2. **Browser** — domain filter, concept list, FTS search (200 ms debounce), detail pane (summary, explanation, Myth/Reality panels, clickable graph cards, RAG chunks with token estimates, source provenance + content_hash). Read-only viewer.
3. **RAG Simulator** — preset queries, token-cap slider 150–600, retrieved chunks vs synthesized *"KUZMIX OFFLINE GROUNDED CONTEXT BLOCK"* (as fed to Android).
4. **Android Query Contract** — renders the sample Room DAO + PRAGMAs + retrieval queries.
5. **Tests** — button → `POST /api/run-tests`, 14-point checklist.

### 11.3 Database schema (3 SQL files)

`001_initial_schema.sql` (PRAGMAs `foreign_keys=ON`, `page_size=4096`) defines **16 tables**:

`metadata` · `domains` · `categories` (FK→domains CASCADE) · `topics` (FK→categories) · `sources` (16 attributed, licensed) · **`concepts`** (canonical_name_normalized, summary_definition, primary_explanation, key_properties/applications/prerequisites JSON, difficulty CHECK introductory→expert, confidence_score REAL 0..1, provenance_type, source_id, content_hash) · **`knowledge_records`** (36-value record_type CHECK: fact, definition, theorem, formula, law, misconception, correction, FAQ, …) · **`chunks`** (`UNIQUE(concept_id, chunk_index)`, `token_estimate`, `header_context`, `embedding_id`) · `entities` (8 types) · **`relationships`** (22-value type CHECK incl. `prerequisite_of`, `used_for`, `contrasts_with`; `UNIQUE(source,target,type)`, weight, is_bidirectional) · **`aliases`** (`UNIQUE(concept_id, alias_normalized)`, 8 alias_types) · **`misconceptions`** (text + correction + explanation + reason_for_confusion) · `disambiguations` · `comparisons` (UNIQUE pair) · `procedures` · `embedding_metadata`.

`002_fts_and_indexes.sql` — **24 B-tree indices** + the FTS5 table (verbatim):

```sql
CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_fts USING fts5(
    concept_id UNINDEXED, record_id UNINDEXED,
    title, content, aliases, category_name, domain_name, keywords,
    tokenize='unicode61 remove_diacritics 2');
```

**It is a plain content-carrying FTS5 table — no `content=` option and no sync triggers.** Rows are inserted manually by the builder (one per knowledge_record + one primary row per concept + rows for disambiguations/comparisons/procedures) = 7,031 rows. Any consumer mutating `concepts`/`knowledge_records` outside the builder silently desyncs `knowledge_fts`.

`003_views_and_contracts.sql` — 7 views: `v_concept_details`, `v_rag_context`, `v_concept_graph`, `v_search_lookup`, `v_disambiguation_lookup`, `v_concept_comparisons`, `v_concept_procedures`.

**Schema versioning:** `PRAGMA user_version = 0`; real version in `metadata.schema_version` = **1.2.0** (bak: 1.0.0); `build_timestamp = 2026-10-04T22:56:47Z`; journal mode **`delete`** (shipped; builder switches WAL→DELETE at the end per app contract).

### 11.4 Build pipeline (`scripts/build_sqlite.py`, 489 lines)

**Sources: 100% local curated/generated JSON — no Wikipedia dumps, no network.**

Ordered steps:
1. Temp DB `/tmp/kuzmix_knowledge_building.sqlite` (page_size 4096, foreign_keys ON, WAL, synchronous NORMAL).
2. Apply `001` → `002` → `003` schema scripts.
3. Ingest `sources.json` (16) → `domains.json` (9) → `categories.json` (61) → `topics.json` (122).
4. Ingest **all `data/concepts/*.json`** (sorted glob, **dedup by cid**): canonical normalization (`name.lower().strip()`), `content_hash = sha256(name:summary:explanation)`, then per concept: aliases, misconceptions, knowledge_records (**+1 FTS row each**, keywords = record_type), **pre-authored chunks** (`token_estimate = max(1, len//4)` if absent, header `"{domain} > {category} > {name}"`), entities, delayed relationship/prerequisite edges, and finally **+1 primary FTS row** (content = summary+explanation, aliases joined, keywords = applications).
5. Second pass: relationships from `cross_domain_links.json` (10 links, weights 0.9–0.95) + prerequisites materialized as `prerequisite_of` (weight 0.95); unknown/duplicate edges skipped.
6. Ingest `disambiguations` (34) + `comparisons` (8) + `procedures` (4), each +1 FTS row.
7. `embedding_metadata` (8 rows: model `text-embedding-004`, 768 dims, **`vector_json: null` — no vectors exist; embeddings are placeholder-only**).
8. COMMIT → write `metadata` (schema_version 1.2.0, build_timestamp, all `total_*` counters, engine target `Android SQLite / Room 2.5+ / Node:sqlite`, `min_android_api_level = 28`, FTS module).
9. `knowledge_fts … VALUES('optimize')`, `PRAGMA optimize`, `wal_checkpoint(TRUNCATE)`, **switch to `journal_mode=DELETE`**.
10. Assert `integrity_check = ok` and `foreign_key_check = 0`.
11. `shutil.copy2` to **both** destinations (removing stale `-shm/-wal`), print size + totals.

**Supporting scripts:** `update_taxonomy.py` (61 categories across 9 domains), `generate_topics.py` (2 topics/category → 122), `generators/` (9 files — domain group generators, `common.py` with template fallbacks incl. 2 template chunks), `generate_all_2500.py` (full corpus assembler — **deletes** existing concept JSONs, regenerates 2,500+), `enrich_corpus.py` (boilerplate → deep encyclopedic rewrite), `apply_rewrites.py` (ground-truth batches, idempotent, regenerates records/chunks/FTS satellites), `generate_intelligence_data.py` (48,984 B — synthesizes disambiguations/comparisons/procedures/families/links/embedding metadata with FK-safe matcher), `build_database.ts` (thin `execSync('python3 …')` wrapper).

### 11.5 Corpus content & taxonomy

**9 domains → 61 categories → 122 topics → 2,577 concepts:**

1. **Science and Mathematics** (17 categories) — Algebra & Number Theory; Calculus & Analysis; Probability & Statistics; Discrete Math & Logic; Classical & Continuum Mechanics; Thermodynamics & Statistical Physics; Electromagnetism & Wave Optics; Quantum & Relativistic Physics; General & Inorganic Chemistry; Organic & Biological Chemistry; Physical Chemistry & Electrochemistry; Cellular & Molecular Biology; Genetics & Evolutionary Biology; Ecology & Environmental Biology; Geology & Geophysics; Atmospheric Science & Meteorology; Astronomy & Astrophysics.
2. **Engineering and Technology** (6) — Mechanical & Aerospace; Electrical & Electronics; Civil & Structural; Materials Science; Robotics & Control; Energy Systems.
3. **Computing and Artificial Intelligence** (8) — CS Foundations; Data Structures & Algorithms; OS & Hardware; Networking; Distributed Systems & Cloud; Cybersecurity & Cryptography; Software Engineering; AI & Machine Learning.
4. **Business and Economics** (6) — Microeconomics; Macroeconomics; Corporate Finance; Accounting; Strategic Management; Marketing.
5. **Human Knowledge and Humanities** (5) — Epistemology & Metaphysics; Ethics; World History & Historiography; Cognitive & Behavioral Psychology; Linguistics & Semantics.
6. **Society, Civics and Culture** (4) — Civics & Governance; Jurisprudence; International Relations; Sociology.
7. **Health and Life Sciences** (5) — Anatomy; Physiology & Neurobiology; Nutrition & Metabolism; First Aid & Emergency; Public Health & Epidemiology.
8. **Creative Fields and Arts** (5) — Visual Design; Typography; Architectural Design; Music Theory; Narrative Structure.
9. **Practical and Applied Knowledge** (5) — Tools & Hardware; Everyday Mechanical Advantage; Domestic Electrical Safety; Culinary Science; Safety & Life Skills.

**Concept distribution:** flagship 27 · sci_math 714 · computing_ai 336 · eng_tech 252 · business_econ 252 · humanities 210 · creative_arts 210 · health_life 208 · society_culture 168 · practical_life 199 · astronomy expansion 1 → **2,577**.

**16 attributed sources** (`data/sources.json`, with licenses): Feynman Lectures; Halliday & Resnick; OpenStax Biology (**CC BY 4.0**); Campbell Biology; OpenStax Chemistry (**CC BY 4.0**); CLRS (MIT Press); Tanenbaum OS; Kurose & Ross; Russell & Norvig AI; Mankiw Economics; WHO Guidelines; Red Cross First Aid; Norman Design; NIST Standards (**Public Domain**); Strang Linear Algebra; McGee Food & Cooking.

**No standalone Geography category** (closest: Geology & Geophysics / World History).

### 11.6 Validation & DB tests

**`validation/validate_database.ts` (9 checks):** integrity_check ok; foreign_key_check = 0; orphaned categories/concepts = error; dangling relationships = error, self-loops = warning; thin content (`summary < 10 chars` or `explanation < 20 chars`) = error; confidence outside 0..1 = error; **FTS coverage `COUNT(knowledge_fts) < COUNT(concepts)` = error**; non-sequential chunk indices = warning. Returns `{passed, errors, warnings, metrics}`.

**`tests/test_database.ts` — 14 tests:**
- *Schema:* file > 50 KB; integrity ok; all 13 mandatory tables; 4 views exist.
- *Contract ops:* `c_gravity` details ("Gravity and Gravitation", sci_math, has records/chunks/relationships); lowercase canonical lookup; alias `"F=ma"` → `c_newtons_laws`; FTS `'electromagnetism OR circuit OR resistance'` includes `c_ohms_law`/`c_domestic_electrical`; `buildRagContext(..., 300)` returns chunks with `estimatedTokens <= 300`; `listDomainsWithStats()` ≥ 8 domains each with concepts.
- *Mutation sandbox* (copied DB): FK enforcement rejects orphan; duplicate alias UNIQUE; duplicate relationship UNIQUE; cascade delete leaves 0 aliases/0 chunks.

### 11.7 Rewrite batches & version history

| Batch | File | # | Scope |
|---|---|---|---|
| 001 | `batch_001_econ_foundations.json` | 20 | Micro/macro foundations: Law of Demand/Supply, equilibrium, elasticity, surplus, deadweight loss, externalities, public goods, GDP, inflation, unemployment, monetary/fiscal policy, money multiplier, business cycle… |
| 002 | `batch_002_cs_science_finance.json` | 6 | TCP/IP, HTTP methods, SQL injection, AES, photosynthesis, compound interest |
| 003 | `batch_003_prob_stats.json` | 14 | Bayes, CLT, normal distribution, LLN, hypothesis testing, CIs, Type I/II, correlation-not-causation, regression, Monte Carlo, MLE, Markov chains, conditional expectation, PDF |
| 004 | `batch_004_mechanics.json` | 14 | Newton's 2nd, momentum, work–energy, angular momentum, SHM, friction, projectile, collisions, escape velocity, Kepler, Hooke, torque, pendulum, center of mass |

**54 concepts rewritten so far** — README: *"remaining template concepts are scheduled for later batches."*

**Database versions (artifact evidence):**
- **1.0.0** — `pre-improve.bak` (20,500,480 B; build 2026-09-25; records 2,580; chunks 5,128; FTS 5,157; aliases 5,177).
- **1.2.0** — current (40,189,952 B; build 2026-10-04; records **4,408**; chunks **8,891**; FTS **7,031**; aliases **6,134**; concept count unchanged at 2,577) — a record/chunk/alias richness expansion (1.1 not observed in artifacts).
- **Android copy version:** `KnowledgeCoreStore.DB_VERSION = 4` (independent integer; tracked in DataStore `knowledge_db_version`).

---

## 12. Android ↔ Knowledge Core Contract

`kuzmix-knowledge/docs/ANDROID_QUERY_CONTRACT.md` (216 lines) freezes the SQL interface. Design principles: (1) zero full-table scans — only B-Tree indices or FTS5; (2) never cache full rows — use pre-computed `chunks`; (3) memory-mapped I/O; (4) read-heavy (`query_only=ON`).

**Standard PRAGMAs on open:** `foreign_keys=ON` · `temp_store=MEMORY` · `query_only=ON` · `mmap_size=268435456` (256 MB).
*(The web UI's Android tab additionally shows `cache_size = -2000` — not in the contract file; minor doc inconsistency.)*

| # | Purpose | App implementation |
|---|---|---|
| **A** | Concept details by ID via `v_concept_details` | used via `rowsByIds` view/joins |
| **B** | Exact name OR alias lookup (`UNION`, `LIMIT 1`) | `KnowledgeCoreStore.lookupExact` — implemented |
| **C** | Definition-only ultra-low-memory select | mapper path |
| **D** | **FTS5 BM25 ranked search (revised, windowed)** | `KnowledgeCoreStore.searchIds` — exact match incl. weights `(0,0,10,1,8,1,1,4)` and `window = min(limit*12, 600)` |
| **E** | RAG chunk retrieval ordered by `chunk_index` | web RAG simulator + contract |
| **F** | Misconceptions & corrections | `rowsByIds` misconception read |
| **G** | Relationship traversal (outgoing UNION ALL incoming prerequisites) | `rowsByIds` relationships read |
| **H** | Domain/category paginated listing | — |

**Memory checklist:** cursors in `use {}`; never `SELECT *`; result sets ≤ 20 via LIMIT/OFFSET; rely on `token_estimate`.

**App conformance verified:** Query D implemented verbatim (window + weights + subquery, with the `bm25`-context error string documented in KDoc); Query B in `lookupExact`; PRAGMA list applied on first open; `DB_VERSION` forces re-copy; FTS5 capability probe; **fallback to Room/JSON if anything fails.**

---

## 13. Knowledge Search Quality Engineering (Recent Work)

The most recent engineering effort (three phases, fully executed and verified) fixed **search starvation**, **added home knowledge search**, and **added theme switching**.

### The problem

Row-level `LIMIT` in the FTS query meant concepts with many indexed rows (or noisy tokens) consumed all result slots, starving unique-concept recall. Evidence probes against the shipped asset:
- `"water"` → **39 unique concepts** (old) vs **50** (windowed) — windowed+weighted SQL verified against the real DB.
- Prefix `"mito"*` → returns matches (previously 0).
- `"laws of motion"` → **1 hit** after `phraseText` stopword-preserving strip (previously 0).

### Phase 1 — search core
1. **`KnowledgeCoreStore.searchIds`** → windowed subquery: inner `ORDER BY bm25(…) LIMIT window` (`window = limit*12`, capped at `MAX_ROW_WINDOW = 600`), outer `GROUP BY concept_id ORDER BY MIN(r)`. (Direct `MIN(bm25(...))` is illegal in SQLite.)
2. **`QueryNormalizer.phraseText`** — leading interrogative + auxiliary strip, **stopwords kept**.
3. **`FtsMatcher.prefixMatch`** + **`inflectionVariants`** (skips `-is`/`-us`/`-ss` endings and tokens < 3).
4. **`KnowledgeCoreConceptSource.candidateIds`** — 4-tier cascade (raw phrase → OR tokens → inflection if `< limit/2` → prefix if `< limit/2`), each tier fetching the full limit, `LinkedHashSet` dedupe.
5. **`KnowledgeIndex`** — `ftsRank` prior (weight **0.06**, position-based), coverage gate (`semantic < 0.33 && !nameHit && !aliasHit → skip`), static/dynamic dedupe by id; weights rebalanced to **lexical .24 / alias .18 / semantic .28 / context .14 / intent .10 / fts .06**.
6. `ANDROID_QUERY_CONTRACT.md` Query D updated to the revised SQL.

### Phase 2 — home knowledge search
`KnowledgeEngine.searchPreview(query, limit)` → `KnowledgeHit(id, name, summary, score)` (minScore **0.15**, `Dispatchers.Default`); `AIRepository.searchKnowledge`; `HomeViewModel` `_searchQuery` + **250 ms debounce** + `mapLatest` → `knowledgeHits`; `HomeScreen` controlled `searchQuery` + "Knowledge" section with `KnowledgeHitItem` (School icon, orange container); **hand-off:** `MainViewModel.setPendingInitialMessage(q)` + `createConversation(title = q.take(60))` → `consumePendingInitialMessage()` (one-shot) → `ChatScreen(initialMessage)` → `ChatViewModel.loadConversation` auto-sends after state load.

### Phase 3 — theme switching
`Color.kt` rewritten (KuzmixColors data class + dark/light vals + `LocalKuzmixColors` + 13 composable accessors, 288 refs migrated); `Theme.kt` (`buildDarkScheme`/`buildLightScheme` + `KuzmixTheme(darkTheme=…)` + CompositionLocalProvider); `MainActivity` collects appearance flow and re-applies `enableEdgeToEdge` per theme (initial compile error — `enableEdgeToEdge` receiver mismatch — fixed via `LocalContext.current as? ComponentActivity`); Profile caption → *"Your choice applies immediately and is saved for future launches."*; `ChatScreen` success color hardcoded `0xFF34D399` → `KuzmixSuccess`.

### Verification
- Tests: **87 → 100** (+5 `KnowledgeIndexTest`, +5 `QueryNormalizerTest`, `FtsMatcherTest` extended to 11).
- `build_apk13`: `BUILD SUCCESSFUL`, `EXIT=0`; APK 26,052,399 B; embedded DB SHA **unchanged** (`9788BC84…69E3`).

---

## 14. Testing Strategy (100 Tests)

Plain **JUnit 4** (no Robolectric, no Mockito, no Android framework deps), run via `gradlew testDebugUnitTest`. Results verified from `app/build/test-results/testDebugUnitTest/*.xml`: **100 tests, 0 failures, 0 errors.**

| File | # | Covers |
|---|---|---|
| `RegressionTest.kt` | **23** | Loads **real raw JSON packs**; intent routing (biology→SUBJECT_DEFINITION provider "LOCAL", alias "bio", capability, study help activates subject, roadmap context, government); 5 code quick-prompts never fall back; `7x8=56` CALCULATION; **9 knowledge-pack acceptance tests** (photosynthesis, mitosis/meiosis comparison, atom/ion/force, Newton's 2nd "F = ma", math/CS/probability, algorithm/database/API, Python/JavaScript/Android, democracy/demand/metaphor/climate/soil, cybersecurity/encryption); greeting exact string; identity mentions Kuzmix AI/Oni Oluwatobi/offline; Nigerian-English **"wetin you fit teach?"**; fallback loop protection (A then B); `12*8=96`, `100/4=25`; help lists capabilities |
| `KnowledgeCoreMapperTest.kt` | 12 | field mapping, definition fallback, caps (examples 3, related 8, misconceptions 4), malformed JSON, formula preference, law-record parsing, confidence labels |
| `FtsMatcherTest.kt` | **11** | empty/stopword→null; **quoting prevents FTS operator injection**; embedded quotes; dedupe + 12-term cap; OR build; phrase adjacency; prefix `*` + ≥3-char rule; inflection plurals; Latin endings skipped |
| `GrammarCheckerTest.kt` | 10 | every rule incl. decimals-not-flagged |
| `DomainBridgeTest.kt` | 9 | subject↔domain bridging incl. biology→health_life+sci_math, general→all |
| `KnowledgePragmasTest.kt` | 7 | returnsRows classification, execSQL safety, FIRST_OPEN sequence |
| `KnowledgeSeederTest.kt` | 6 | import-once, idempotent re-run, replace-on-reseed, comma-containing lists round-trip, FTS search, multi-word alias adjacency (fake DAOs) |
| **`KnowledgeIndexTest.kt`** | **5** | ftsRank prior ordering/raises score; coverage gate drops 1/4-token noise but keeps name hit; single-token never gated; static+dynamic dedupe; legacy tie equality |
| **`QueryNormalizerTest.kt`** | **5** | stopwords kept in phraseText; interrogative stripped; auxiliary stripped; punctuation trimmed; empty input |
| `RoomConceptSourceTest.kt` | 5 | phrase tier first (multi-word alias survives LIMIT); stopwords never hit FTS; cache reuse (second query no refetch) |
| `SpeechInputControllerTest.kt` | 4 | `mergeDictation` fill/append/trim/blank |
| `ConceptCacheTest.kt` | 3 | capacity (40 inserts → 30 kept), access-refresh LRU, getOrPut |

No Room DAO instrumentation tests, no UI/theme tests (the `androidTest` source set does not exist).

---

## 15. Documentation Set

| Document | Location | Size / scope |
|---|---|---|
| **User Manual** | `docs/USER_MANUAL.md` | 19,480 B; 12 sections: What Is Kuzmix AI, Getting Started, Navigating, Home, Chatting, Voice Input, Your Chats, Tools, Profile & Settings, Capabilities/Limitations, Troubleshooting, Privacy & Data + cheat sheet. States 2,577 concepts, no-INTERNET, airplane-mode operation. |
| **This document** | `docs/PROJECT_DOCUMENTATION.md` | full technical reference |
| **Android Query Contract** | `kuzmix-knowledge/docs/ANDROID_QUERY_CONTRACT.md` | 6,484 B; pragmas + Queries A–H + memory checklist |
| **Schema Architecture** | `kuzmix-knowledge/docs/SCHEMA_ARCHITECTURE.md` | 5,959 B; ASCII ER diagram & specs |
| **Knowledge Core README** | `KUZMIX_KNOWLEDGE_README.md` | 9,182 B; purpose, metrics table, build commands A–G |
| Template README | knowledge-core `README.md` | 542 B (Google AI Studio residue) |
| Code docs | KDoc comments throughout | e.g. `searchIds` KDoc documents the window rationale and probe numbers |

**No root README, no CHANGELOG, no git history anywhere** — version history is reconstructed from artifacts (section 18).

---

## 16. Security, Privacy & Offline Guarantees

1. **No network attack surface:** `INTERNET` permission is absent — the OS blocks all socket access. No telemetry, no analytics, no cloud calls, no ads.
2. **Read-only knowledge DB:** `PRAGMA query_only=ON` + FTS probe + versioned asset re-copy; integrity asserted at build time (`integrity_check`, `foreign_key_check`).
3. **Data stays on device:** conversations live in Room `kuzmix.db`; preferences in DataStore `kuzmix_prefs`; `allowBackup=true` (default Android backup behavior — note: no custom `dataExtractionRules` declared).
4. **Voice privacy:** on-device `SpeechRecognizer` only (`createOnDeviceSpeechRecognizer` / `EXTRA_PREFER_OFFLINE`); the only runtime permission is `RECORD_AUDIO`.
5. **Secrets scan:** **no API keys or secrets exist anywhere** in the workspace. `.env.example` holds only placeholders; no `.env.local` exists; the server never calls Gemini.
6. **Content safety:** misconceptions/corrections guardrail, confidence gating, fallback-loop protection, honest error copy for missing models.

---

## 17. Known Limitations, Stubs & Inconsistencies

**Deliberate stubs (honest in UI):**
- `LocalAIEngine` — `isAvailable=false`; awaiting a bundled on-device model (llama.cpp/GGUF/ONNX/LiteRT).
- Image Generator — reports *"Local image model not installed…"*; no generation.
- Translator — *"requires a local language model. Coming soon."*
- More Tools — *"on the way."*
- Profile stats (Projects 12 / Chats 48 / Tools 15) — **hard-coded display values**, not computed.
- Default user identity baked into DataStore defaults (name "Oni Oluwatobi", email thekreadivegalaxy@gmail.com).

**Real limitations:**
- **No standalone Geography category**; no Nigerian-specific curriculum in the 2,577 corpus (the old Python prototype had forestry/CRS content that never migrated).
- **Embeddings are aspirational:** `embedding_metadata` rows exist (text-embedding-004, 768 dims) but `vector_json = null`, `chunks.embedding_id = NULL`, and no embedding step exists — retrieval is lexical/BM25 only.
- **FTS desync hazard:** `knowledge_fts` has no triggers; only the builder/importer may mutate tables.
- **No instrumentation/UI tests; no CI; no git repo.**
- **`androidTest` source set does not exist.**

**Documented inconsistencies (left as-is, flagged):**

| Item | Skew |
|---|---|
| Version numbers | DB `schema_version 1.2.0` vs web UI badge "SQLite Core v1.0" vs Android `DB_VERSION 4` vs package.json `0.0.0` vs app `1.0.0` |
| Bundle size | USER_MANUAL says ~19.6 MB; actual asset is 40,189,952 B (38.33 MB) — 19.6 MB approx. equals the old 20,500,480-B v1.0.0 DB; likely stale doc figure |
| `PRAGMA cache_size=-2000` | shown in web UI Android tab, absent from contract .md |
| `database/kuzmix-db.json` | 73-B empty server stub, unused by the app |
| Prototype `main.py` | references undefined `VARIATIONS`/`RESPONSES` → would `NameError` at runtime (mid-refactor state); prototype is not part of the build |
| `.reflex/`, `metadata.json` Gemini capability | AI-Studio/Reflex template residue, unused |
| README.md (knowledge-core) | generic template; real docs are in `KUZMIX_KNOWLEDGE_README.md` |

---

## 18. Project History / Timeline

Reconstructed from artifacts (no git exists):

1. **Python prototype era** — `knowledge_base.py` (21 KB dict incl. forestry/CRS/Nigerian-curriculum content), `main.py` CLI bot, `rag_engine.py` (sentence-transformers `all-MiniLM-L6-v2`, pickled vector index `kuzmix_rag_db.pkl`). All superseded; retained as ancestors. Attribution strings: *"built by Oni Oluwatobi under The Kreadive Galaxy."*
2. **Knowledge core (2026-09-25)** — SQLite schema v**1.0.0** built (20,500,480 B snapshot preserved as `pre-improve.bak`).
3. **Corpus improvement pass (2026-10-04)** — rewrite batches 001–004 (54 concepts), enrichment, record/chunk/alias expansion → schema v**1.2.0**, 40,189,952 B. `DB_VERSION` on the Android side advanced to **4**.
4. **Android app build iterations** — 13 `build_apk*.bat` scripts + 11 `run_tests*.bat` scripts with logs; script evolution: `--no-daemon` era (1–4) → plain (5–7) → **test+assemble gating (8–13)**; notable failures (`build-err.log` 42m 29s FAILED) and successes (`apk10` 9m19s, `apk13` 7m46s, EXIT=0).
5. **Search-quality project (recent)** — Phase 1 search core (windowed BM25, phraseText, 4-tier candidates, ftsRank prior, coverage gate, rebalanced weights), Phase 2 home knowledge search + chat hand-off, Phase 3 full dark/light/system theme system. Tests 87 → **100**. APK re-assembled (26,052,399 B), DB SHA verified unchanged.
6. **Documentation** — `docs/USER_MANUAL.md` (19,480 B) and this document.

Room DB v1→v2 migration (adding `concepts`/`concepts_fts`) is earlier in-app evidence of the JSON-seed era preceding the SQLite asset era.

---

## 19. Roadmap

Inferred from stubs, KDocs, and corpus plans (not a commitment):

1. **Bundle an on-device generative model** — flip `LocalAIEngine.isAvailable = true` (llama.cpp/GGUF/ONNX/LiteRT); wire `modelStatus` + Settings → AI Model import (UI already exposes it); enables Image Gen and Translator.
2. **Rewrite batches 005+** — replace remaining template concepts (README: *"remaining template concepts are scheduled for later batches"*).
3. **Embeddings** — implement the reserved `embedding_metadata` / `chunks.embedding_id` path (semantic retrieval alongside BM25) if a small on-device embedder ships.
4. **Fix doc skews** — manual size figure (19.6 → 38.33 MB), version-number unification, contract `cache_size` note.
5. **Test/infra** — instrumentation tests for Room + theme; a root README; version control; CI running `testDebugUnitTest` + `db:test` + `db:validate`.
6. **Content expansion** — migrate prototype-only domains (forestry/agriculture/CRS) into the corpus if desired; possibly a Geography category.
7. **Language/localization** — `language` preference exists but only English content ships; Nigerian-English patterns exist in intent matching.

---

## 20. Appendices

### A. Key file index

| Concern | File |
|---|---|
| DI container | `app/src/main/java/com/kuzmix/ai/di/AppContainer.kt` |
| Theme | `…/ui/theme/Color.kt`, `…/ui/theme/Theme.kt` |
| Search SQL | `…/data/knowledge/KnowledgeCoreStore.kt` (`searchIds`, `DB_VERSION=4`, `MAX_ROW_WINDOW=600`) |
| Candidate tiers | `…/data/knowledge/KnowledgeCoreConceptSource.kt` |
| Scoring | `…/domain/ai/KnowledgeIndex.kt` |
| Answer pipeline | `…/domain/ai/KnowledgeEngine.kt` |
| Intent routing | `…/domain/ai/IntentClassifier.kt` |
| Query text prep | `…/domain/ai/QueryNormalizer.kt`, `…/domain/ai/FtsMatcher.kt` |
| Navigation / hand-off | `…/ui/navigation/KuzmixNavGraph.kt`, `…/ui/MainViewModel.kt` |
| Home search | `…/ui/screens/home/HomeViewModel.kt`, `HomeScreen.kt` |
| Voice | `…/voice/SpeechInputController.kt` |
| Corpus asset | `app/src/main/assets/kuzmix_knowledge.sqlite` |
| Contract | `kuzmix-knowledge/docs/ANDROID_QUERY_CONTRACT.md` |
| DB builder | `kuzmix-knowledge/scripts/build_sqlite.py` |
| Schema | `kuzmix-knowledge/schema/001…003*.sql` |

### B. Canonical constants

```
App:          package com.kuzmix.ai · minSdk 26 · targetSdk 36 · compileSdk 37 · v1.0.0(1)
Room:         kuzmix.db · version 2 · MIGRATION_1_2
DataStore:    kuzmix_prefs (8 keys)
Asset DB:     kuzmix_knowledge.sqlite · 40,189,952 B
              SHA-256 9788BC84EDE20C412D7E4CDB5FB7C33C45E4F376F35A29094150F58F587E69E3
              KnowledgeCoreStore.DB_VERSION = 4 · MAX_ROW_WINDOW = 600
BM25 weights: (0, 0, 10.0, 1.0, 8.0, 1.0, 1.0, 4.0)   # title x10, aliases x8, keywords x4
Score:        lexical×0.24 + alias×0.18 + semantic×0.28 + context×0.14 + intent×0.10 + fts×0.06
Gate:         semantic < 0.33 && !nameHit && !aliasHit -> skip
Accept:       answers >=0.28 (definition) / >=0.30 (retrieval) · preview minScore 0.15
Confidence:   >=0.55 HIGH · >=0.32 MEDIUM · else LOW (exact = HIGH)
Tiers:        CANDIDATE_LIMIT 50 · LOOKUP_LIMIT 25 · MAX_TERMS 12
Debounce:     250 ms · min query length 2 · preview limit 5
Cache:        LRU 15 (low-RAM) / 25 (<=128 MB) / 40 / 30 default
PRAGMAs:      journal_mode=DELETE · query_only=ON · temp_store=MEMORY · mmap_size=268435456
Corpus:       9 domains · 61 categories · 122 topics · 2,577 concepts · 4,408 records
              8,891 chunks · 2,617 relationships · 6,134 aliases · 2,587 misconceptions
              7,031 FTS rows · 16 sources · schema_version 1.2.0 · build 2026-10-04T22:56:47Z
Tests:        100 (12 files) · DB suite 14 · validator 9 checks
APK:          app-debug.apk · 26,052,399 B · build_apk13 EXIT=0
```

### C. Routes

`welcome` · `home` · `chats` · `tools` · `profile` · `chat/{conversationId}` · `image_gen` · `code_assistant`
Bottom bar visible on: `home | chats | tools | profile`.

### D. Glossary

- **Concept** — a knowledge node (definition, explanation, aliases, misconceptions, records, chunks).
- **Knowledge record** — a typed fact/statement attached to a concept (36 types).
- **Chunk** — a pre-authored RAG-sized text segment with `token_estimate` (approx. chars/4).
- **FTS5 row** — a manually inserted index row (per record, per concept, per satellite row) in `knowledge_fts`.
- **Tier (candidate)** — progressive recall strategy: phrase → OR tokens → inflections → prefix.
- **Coverage gate** — drops matches covering less than 1/3 of query tokens unless name/alias hits.
- **ftsRank prior** — position-based score (`1 − i/n`) from FTS results, blended at weight 0.06.
- **Fallback pair** — FALLBACK_A/B strings with loop protection when retrieval fails.
- **DB_VERSION (4)** — Android asset-copy version; bump together with the bundled asset.

---

*End of document. Prepared from direct source inspection of the Android app, the knowledge-core subproject, build logs, test result XMLs, and live read-only queries against the shipped SQLite corpus.*
