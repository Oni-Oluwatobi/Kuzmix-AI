import { useEffect, useState } from "react";
import {
  Database,
  Search,
  Layers,
  Share2,
  CheckCircle2,
  Download,
  Smartphone,
  ShieldCheck,
  AlertTriangle,
  BookOpen,
  ArrowRight,
  Sparkles,
  Zap,
  Play,
  Cpu,
  Clock,
  Sigma,
  XCircle,
  CircleDashed,
  FlaskConical
} from "lucide-react";
import { AuthProvider, useAuth } from "./context/AuthContext";
import AuthModal from "./components/AuthModal";
import AndroidContractTab from "./components/AndroidContractTab";
import {
  Card,
  CopyButton,
  CodeBlock,
  MetricCard,
  Pill,
  SectionTitle,
  StatusDot
} from "./components/ui";
import { initAnalytics } from "./firebase";

interface StatsData {
  fileSizeBytes: number;
  fileSizeKB: string;
  domains: number;
  categories: number;
  topics: number;
  concepts: number;
  records: number;
  chunks: number;
  relationships: number;
  aliases: number;
  misconceptions: number;
  entities: number;
  sources: number;
  ftsEntries: number;
  integrity: string;
  schemaVersion?: string;
}

interface DomainItem {
  id: string;
  name: string;
  slug: string;
  description: string;
  icon: string;
  category_count: number;
  concept_count: number;
}

interface ConceptSummary {
  id: string;
  name: string;
  summary_definition: string;
  difficulty_level: string;
  confidence_score: number;
  domain_name: string;
  category_name: string;
  domain_id: string;
  category_id: string;
}

interface TestCaseView {
  name: string;
  status?: "passed" | "failed";
  durationMs?: number;
}

const TEST_CATALOG: { name: string; desc: string }[] = [
  {
    name: "Database file exists and has non-zero size",
    desc: "Confirms kuzmix_knowledge.sqlite ships complete inside the deployment artifact."
  },
  {
    name: "SQLite PRAGMA integrity_check passes",
    desc: "Verifies low-level SQLite B-tree page consistency and absence of file corruption."
  },
  {
    name: "All mandatory tables exist in sqlite_master",
    desc: "Validates presence of domains, categories, topics, concepts, chunks, records and FTS5."
  },
  {
    name: "Required convenience views exist",
    desc: "Checks v_concept_details, v_rag_context, v_concept_graph, v_search_lookup."
  },
  {
    name: "Exact concept lookup by ID (e.g., c_gravity)",
    desc: "Verifies c_gravity lookup with linked records, chunks and relationships."
  },
  {
    name: "Case-insensitive concept lookup by name",
    desc: "Ensures case-folding and trim normalization work for O(1) matching."
  },
  {
    name: 'Concept resolution via alias (e.g., "F=ma")',
    desc: "Tests search alias index resolves colloquial query to c_newtons_laws."
  },
  {
    name: "Full-Text Search (FTS5) BM25 ranking",
    desc: "Ensures ranking order prioritizes high-relevance title and content matches."
  },
  {
    name: "RAG Context Assembly fits token constraints",
    desc: "Validates pre-computed token limits prevent prompt overflow on small-context LLMs."
  },
  {
    name: "Multi-domain coverage breadth",
    desc: "Asserts every domain is populated with non-zero concept counts."
  },
  {
    name: "Foreign key enforcement blocks orphaned child insert",
    desc: "Guarantees child records cannot reference non-existent parents."
  },
  {
    name: "Duplicate alias collision constraint",
    desc: "Rejects duplicate alias rows through UNIQUE index enforcement."
  },
  {
    name: "Deduplication prevents identical relationship duplicates",
    desc: "Prevents duplicate identical relationships and content collisions."
  },
  {
    name: "Cascade delete removes records, aliases, and chunks cleanly",
    desc: "Ensures deleting a concept cleanly removes its chunks, aliases and misconceptions."
  }
];

function parseJsonArray<T>(raw: unknown): T[] {
  try {
    const value = JSON.parse(String(raw ?? "[]"));
    return Array.isArray(value) ? (value as T[]) : [];
  } catch {
    return [];
  }
}

function formatDbSizeMB(bytes?: number): string {
  if (!bytes) return "40.18";
  return (Math.floor(bytes / 10000) / 100).toFixed(2);
}

export default function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}

function AppShell() {
  const { currentUser, logout } = useAuth();

  const [activeTab, setActiveTab] = useState<
    "overview" | "browser" | "rag" | "android" | "tests"
  >("overview");
  const [stats, setStats] = useState<StatsData | null>(null);
  const [domains, setDomains] = useState<DomainItem[]>([]);
  const [concepts, setConcepts] = useState<ConceptSummary[]>([]);
  const [selectedDomain, setSelectedDomain] = useState<string>("all");
  const [selectedConceptId, setSelectedConceptId] = useState<string | null>("c_gravity");
  const [conceptDetail, setConceptDetail] = useState<any | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [conceptIndex, setConceptIndex] = useState<Record<string, string>>({});

  // RAG Simulator State
  const [ragQuery, setRagQuery] = useState("How does gravity behave in orbit?");
  const [ragTokenLimit, setRagTokenLimit] = useState(350);
  const [ragResult, setRagResult] = useState<any | null>(null);
  const [ragLoading, setRagLoading] = useState(false);

  // Search State
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<any[]>([]);

  // Test Runner State
  const [runningTests, setRunningTests] = useState(false);
  const [testResult, setTestResult] = useState<any | null>(null);
  const [integrity, setIntegrity] = useState<any | null>(null);
  const [validating, setValidating] = useState(false);

  // Auth UI state
  const [authOpen, setAuthOpen] = useState(false);

  // Fire-and-forget analytics (no-op when unsupported / offline dev).
  useEffect(() => {
    initAnalytics().catch(() => null);
  }, []);

  // Load initial stats & domains
  useEffect(() => {
    fetch("/api/stats")
      .then((res) => res.json())
      .then((data) => {
        if (data.success) setStats(data.data);
      })
      .catch(console.error);

    fetch("/api/domains")
      .then((res) => res.json())
      .then((data) => {
        if (data.success) setDomains(data.data);
      })
      .catch(console.error);

    fetchConcepts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Fetch concepts with optional domain filter
  const fetchConcepts = (domainId?: string) => {
    const url =
      domainId && domainId !== "all" ? `/api/concepts?domain=${domainId}` : "/api/concepts";
    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          setConcepts(data.data);
          setConceptIndex((prev) => {
            const next = { ...prev };
            for (const c of data.data as ConceptSummary[]) next[c.id] = c.name;
            return next;
          });
          if (data.data.length > 0 && !selectedConceptId) {
            setSelectedConceptId(data.data[0].id);
          }
        }
      })
      .catch(console.error);
  };

  // Load detailed concept when selected
  useEffect(() => {
    if (!selectedConceptId) return;
    setLoadingDetail(true);
    fetch(`/api/concept/${selectedConceptId}`)
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          setConceptDetail(data.data);
          setConceptIndex((prev) => ({
            ...prev,
            [data.data.concept_id]: data.data.concept_name
          }));
        }
        setLoadingDetail(false);
      })
      .catch(() => setLoadingDetail(false));
  }, [selectedConceptId]);

  // Handle RAG context query
  const executeRagQuery = (queryText: string = ragQuery) => {
    if (!queryText.trim()) return;
    setRagLoading(true);
    fetch(`/api/rag?q=${encodeURIComponent(queryText)}&tokens=${ragTokenLimit}`)
      .then((res) => res.json())
      .then((data) => {
        if (data.success) setRagResult(data.data);
        setRagLoading(false);
      })
      .catch(() => setRagLoading(false));
  };

  // Handle live search (200ms debounce)
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const timer = setTimeout(() => {
      fetch(`/api/search?q=${encodeURIComponent(searchQuery)}`)
        .then((res) => res.json())
        .then((data) => {
          if (data.success) setSearchResults(data.data);
        })
        .catch(console.error);
    }, 200);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Run test suite (+ integrity report)
  const handleRunTests = () => {
    setRunningTests(true);
    fetch("/api/run-tests", { method: "POST" })
      .then((res) => res.json())
      .then((data) => {
        setTestResult(data);
        if (data.integrity) setIntegrity(data.integrity);
        setRunningTests(false);
      })
      .catch((err) => {
        setTestResult({ success: false, error: err.message });
        setRunningTests(false);
      });
  };

  // Standalone integrity check
  const handleValidate = () => {
    setValidating(true);
    fetch("/api/validate")
      .then((res) => res.json())
      .then((data) => {
        if (data.success) setIntegrity(data.data);
        setValidating(false);
      })
      .catch(() => setValidating(false));
  };

  const jumpToConcept = (id: string) => {
    setSearchQuery("");
    setSelectedConceptId(id);
  };

  const schemaVersion = stats?.schemaVersion ?? "1.2.0";
  const dbSizeMB = formatDbSizeMB(stats?.fileSizeBytes);
  const integrityOk = stats?.integrity === "ok";

  const tabs: { id: typeof activeTab; label: string; icon: typeof Layers }[] = [
    { id: "overview", label: "Overview", icon: Layers },
    { id: "browser", label: "Knowledge Browser", icon: BookOpen },
    { id: "rag", label: "RAG Simulator", icon: Sparkles },
    { id: "android", label: "Android Contract", icon: Smartphone },
    { id: "tests", label: "Tests & Integrity", icon: ShieldCheck }
  ];

  return (
    <div className="flex min-h-screen flex-col bg-[#0A0A0F] font-sans text-[#E5E7EB] selection:bg-[#FF8C00]/30">
      {/* ============================= HEADER ============================= */}
      <header className="sticky top-0 z-50 border-b border-[#22222E] bg-[#0A0A0F]/90 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          {/* Branding */}
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#FF8C00] shadow-lg shadow-[#FF8C00]/20">
              <Database className="h-5 w-5 text-[#0A0A0F]" />
            </div>
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-lg font-bold tracking-tight text-white">Kuzmix AI</span>
                <span className="rounded-full border border-[#FF8C00]/40 bg-[#FF8C00]/10 px-2 py-0.5 text-xs font-semibold text-[#FF8C00]">
                  Knowledge Core v{schemaVersion}
                </span>
                <span className="hidden text-xs text-[#6B7280] sm:inline">
                  by The Kreadive Galaxy
                </span>
              </div>
              {/* Global status pill */}
              <div className="mt-0.5 flex flex-wrap items-center gap-2 text-[11px] text-[#9CA3AF]">
                <span className="inline-flex items-center gap-1.5">
                  <StatusDot tone={integrityOk ? "green" : "red"} />
                  SQLite {integrityOk ? "Connected" : "Degraded"}
                </span>
                <span className="text-[#2A2A36]">|</span>
                <span className="font-mono">schema v{schemaVersion}</span>
                <span className="text-[#2A2A36]">|</span>
                <span className="font-mono">{dbSizeMB} MB</span>
              </div>
            </div>
          </div>

          {/* Right cluster: download + auth */}
          <div className="flex items-center gap-2 sm:gap-3">
            <a
              href="/api/download/sqlite"
              download="kuzmix_knowledge.sqlite"
              className="hidden h-10 items-center gap-2 rounded-lg border border-[#FFC107]/50 px-3.5 text-xs font-semibold text-[#FFC107] transition hover:bg-[#FFC107] hover:text-[#0A0A0F] active:scale-95 sm:inline-flex"
            >
              <Download className="h-3.5 w-3.5" />
              <span>Download SQLite ({dbSizeMB} MB)</span>
            </a>

            {currentUser ? (
              <div className="flex items-center gap-2">
                <div
                  className="flex h-9 w-9 items-center justify-center rounded-full bg-[#FF8C00] text-xs font-bold text-[#0A0A0F]"
                  title={currentUser.email ?? "Signed in"}
                >
                  {(currentUser.email?.[0] ?? "U").toUpperCase()}
                </div>
                <span className="hidden max-w-[200px] truncate items-center rounded-lg border border-[#2A2A36] bg-[#1E1E2A] px-3 py-2 text-xs text-[#9CA3AF] md:inline-flex">
                  {currentUser.email}
                </span>
                <button
                  type="button"
                  onClick={() => logout().catch(console.error)}
                  className="h-10 rounded-lg border border-[#2A2A36] px-3.5 text-xs font-semibold text-[#9CA3AF] transition hover:border-[#FF8C00] hover:text-[#FF8C00] active:scale-95"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setAuthOpen(true)}
                className="h-10 rounded-lg bg-[#FF8C00] px-4 text-xs font-semibold text-[#0A0A0F] transition hover:bg-[#FF6A00] active:scale-95"
              >
                Sign In
              </button>
            )}
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="border-t border-[#22222E]">
          <div className="mx-auto flex max-w-7xl space-x-1 overflow-x-auto px-4 sm:space-x-3 sm:px-6 lg:px-8">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex h-11 items-center gap-2 whitespace-nowrap border-b-2 px-3 text-xs font-semibold transition sm:text-sm ${
                    isActive
                      ? "border-[#FF8C00] bg-[#FF8C00]/5 text-[#FF8C00]"
                      : "border-transparent text-[#9CA3AF] hover:border-[#2A2A36] hover:text-white"
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>
      </header>

      {/* ============================= MAIN ============================= */}
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 lg:px-8">
        {/* ---------------- TAB 1: OVERVIEW ---------------- */}
        {activeTab === "overview" && (
          <div className="space-y-6">
            {/* Hero banner */}
            <div className="relative overflow-hidden rounded-2xl border border-[#22222E] bg-[#14141C] p-6 shadow-2xl sm:p-8">
              <div className="pointer-events-none absolute -right-16 -top-16 h-72 w-72 rounded-full bg-[#FF8C00]/10 blur-3xl" />
              <div className="relative max-w-3xl">
                <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-emerald-800/60 bg-emerald-950/60 px-3 py-1 font-mono text-xs text-emerald-400">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  <span>
                    SQLite B-Tree Integrity: {integrityOk ? "OK" : "Checking…"} • Foreign Keys
                    Enforced
                  </span>
                </div>
                <h1 className="text-2xl font-extrabold leading-tight tracking-tight text-white sm:text-4xl">
                  Kuzmix Offline Knowledge Core
                </h1>
                <p className="mt-3 text-sm leading-relaxed text-[#9CA3AF] sm:text-base">
                  The compiled binary database{" "}
                  <code className="rounded bg-[#0A0A0F] px-1.5 py-0.5 text-[#FF8C00]">
                    kuzmix_knowledge.sqlite
                  </code>{" "}
                  ships ready for Android deployment — powering on-device RAG retrieval on
                  low-RAM smartphones with zero cloud connectivity.
                </p>
                <div className="mt-6 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => setActiveTab("rag")}
                    className="inline-flex h-10 items-center gap-2 rounded-lg bg-[#FF8C00] px-4 text-xs font-semibold text-[#0A0A0F] transition hover:bg-[#FF6A00] active:scale-95"
                  >
                    <Zap className="h-4 w-4" />
                    <span>Test Live RAG Retrieval</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveTab("browser")}
                    className="inline-flex h-10 items-center gap-2 rounded-lg border border-[#2A2A36] bg-[#1E1E2A] px-4 text-xs font-semibold text-white transition hover:border-[#FF8C00] active:scale-95"
                  >
                    <BookOpen className="h-4 w-4" />
                    <span>Browse {(stats?.concepts ?? 2577).toLocaleString()} Concepts</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveTab("tests")}
                    className="inline-flex h-10 items-center gap-2 rounded-lg border border-[#2A2A36] bg-[#1E1E2A] px-4 text-xs font-semibold text-white transition hover:border-[#FF8C00] active:scale-95"
                  >
                    <ShieldCheck className="h-4 w-4" />
                    <span>Run Test Suite</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Primary metrics */}
            <div>
              <SectionTitle hint="Live counts from the SQLite artifact">
                Corpus Metrics
              </SectionTitle>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5">
                <MetricCard
                  label="Concepts"
                  value={(stats?.concepts ?? 2577).toLocaleString()}
                  hint="Attributed nodes"
                />
                <MetricCard
                  label="Records"
                  value={(stats?.records ?? 4408).toLocaleString()}
                  hint="Fine-grained facts"
                />
                <MetricCard
                  label="Chunks"
                  value={(stats?.chunks ?? 8891).toLocaleString()}
                  hint="100–250 tokens each"
                />
                <MetricCard
                  label="FTS Rows"
                  value={(stats?.ftsEntries ?? 7031).toLocaleString()}
                  hint="BM25 indexed"
                />
                <MetricCard
                  label="Sources"
                  value={(stats?.sources ?? 16).toLocaleString()}
                  hint="Honest OER provenance"
                />
              </div>
            </div>

            {/* Secondary strip */}
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
              {[
                { label: "Database Size", val: `${dbSizeMB} MB` },
                { label: "Broad Domains", val: stats?.domains ?? 9 },
                { label: "Disciplines", val: stats?.categories ?? 61 },
                { label: "Graph Relationships", val: (stats?.relationships ?? 2617).toLocaleString() },
                { label: "Search Aliases", val: (stats?.aliases ?? 6134).toLocaleString() },
                { label: "Misconceptions", val: (stats?.misconceptions ?? 2587).toLocaleString() }
              ].map((item) => (
                <div
                  key={item.label}
                  className="rounded-lg border border-[#22222E] bg-[#14141C] p-3 text-center"
                >
                  <div className="text-lg font-bold text-white">{item.val}</div>
                  <div className="text-xs text-[#9CA3AF]">{item.label}</div>
                </div>
              ))}
            </div>

            {/* Domain grid */}
            <div>
              <SectionTitle
                hint="Zero single-domain bias"
                action={
                  <a
                    href="/api/download/sqlite"
                    download="kuzmix_knowledge.sqlite"
                    className="inline-flex h-9 items-center gap-2 rounded-lg border border-[#FFC107]/50 px-3 text-xs font-semibold text-[#FFC107] transition hover:bg-[#FFC107] hover:text-[#0A0A0F] active:scale-95"
                  >
                    <Download className="h-3.5 w-3.5" />
                    Quick Download SQLite
                  </a>
                }
              >
                Active Knowledge Domains
              </SectionTitle>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {domains.map((dom) => (
                  <Card
                    key={dom.id}
                    hover
                    className="cursor-pointer p-4"
                  >
                    <div onClick={() => {
                      setSelectedDomain(dom.id);
                      fetchConcepts(dom.id);
                      setActiveTab("browser");
                    }} className="group">
                      <div className="flex items-start justify-between gap-2">
                        <div className="text-sm font-semibold text-white transition group-hover:text-[#FF8C00]">
                          {dom.name}
                        </div>
                        <span className="shrink-0 rounded bg-[#FF8C00]/10 px-2 py-0.5 font-mono text-xs text-[#FF8C00]">
                          {dom.concept_count} concepts
                        </span>
                      </div>
                      <p className="mt-2 line-clamp-2 text-xs leading-relaxed text-[#9CA3AF]">
                        {dom.description}
                      </p>
                      <div className="mt-3 flex items-center justify-between border-t border-[#22222E] pt-2 text-[11px] text-[#6B7280]">
                        <span>{dom.category_count} categories</span>
                        <span className="flex items-center font-medium text-[#FF8C00] transition group-hover:translate-x-0.5">
                          Explore <ArrowRight className="ml-1 h-3 w-3" />
                        </span>
                      </div>
                    </div>
                  </Card>
                ))}
                {domains.length === 0 && (
                  <div className="col-span-full rounded-xl border border-[#22222E] bg-[#14141C] p-6 text-center text-xs text-[#6B7280]">
                    Loading domains from SQLite…
                  </div>
                )}
              </div>
            </div>

            {/* Architecture highlights */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <Card className="p-5">
                <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-lg bg-[#FF8C00]/15">
                  <Smartphone className="h-4 w-4 text-[#FF8C00]" />
                </div>
                <h3 className="text-sm font-semibold text-white">Engineered for Low-RAM Android</h3>
                <p className="mt-1 text-xs leading-relaxed text-[#9CA3AF]">
                  SQLite FTS5 with unicode61 tokenizers and memory-mapped I/O (
                  <code className="text-[#D1D5DB]">PRAGMA mmap_size</code>). Kuzmix never loads
                  the full database into memory — only exact matching records.
                </p>
              </Card>
              <Card className="p-5">
                <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-900/50">
                  <ShieldCheck className="h-4 w-4 text-emerald-400" />
                </div>
                <h3 className="text-sm font-semibold text-white">Explicit Anti-Hallucination Data</h3>
                <p className="mt-1 text-xs leading-relaxed text-[#9CA3AF]">
                  Every concept maps common misconceptions to verified corrections and explains
                  why errors happen. Sources are honest (OpenStax, Feynman Lectures, WHO, MIT
                  Press) without fabricated citations.
                </p>
              </Card>
              <Card className="p-5">
                <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-lg bg-[#FFC107]/15">
                  <Share2 className="h-4 w-4 text-[#FFC107]" />
                </div>
                <h3 className="text-sm font-semibold text-white">Expandable via Knowledge Packs</h3>
                <p className="mt-1 text-xs leading-relaxed text-[#9CA3AF]">
                  The <code className="text-[#D1D5DB]">import_pack.ts</code> tooling merges
                  specialty knowledge packs with duplicate detection and zero schema
                  alterations.
                </p>
              </Card>
            </div>
          </div>
        )}

        {/* ---------------- TAB 2: KNOWLEDGE BROWSER ---------------- */}
        {activeTab === "browser" && (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
            {/* Left: sticky filters + concept list */}
            <div className="lg:col-span-4">
              <div className="flex max-h-[calc(100vh-160px)] flex-col gap-3 lg:sticky lg:top-[124px]">
                {/* Domain filter pills */}
                <div>
                  <label className="mb-2 block text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                    Filter by Domain
                  </label>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedDomain("all");
                        fetchConcepts("all");
                      }}
                      className={`h-8 rounded-full border px-3 text-[11px] font-semibold transition ${
                        selectedDomain === "all"
                          ? "border-[#FF8C00] bg-[#FF8C00] text-[#0A0A0F]"
                          : "border-[#2A2A36] bg-[#1E1E2A] text-[#9CA3AF] hover:border-[#FF8C00] hover:text-[#FF8C00]"
                      }`}
                    >
                      All ({(stats?.concepts ?? 2577).toLocaleString()})
                    </button>
                    {domains.map((d) => (
                      <button
                        key={d.id}
                        type="button"
                        onClick={() => {
                          setSelectedDomain(d.id);
                          fetchConcepts(d.id);
                        }}
                        title={`${d.concept_count} concepts`}
                        className={`h-8 rounded-full border px-3 text-[11px] font-semibold transition ${
                          selectedDomain === d.id
                            ? "border-[#FF8C00] bg-[#FF8C00] text-[#0A0A0F]"
                            : "border-[#2A2A36] bg-[#1E1E2A] text-[#9CA3AF] hover:border-[#FF8C00] hover:text-[#FF8C00]"
                        }`}
                      >
                        {d.name}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Search input (200ms debounced) */}
                <div className="relative">
                  <Search className="absolute left-3 top-3 h-4 w-4 text-[#6B7280]" />
                  <input
                    type="text"
                    placeholder="Search 7,031 FTS rows…"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="h-10 w-full rounded-lg border border-[#2A2A36] bg-[#14141C] pl-9 pr-3 text-xs text-white outline-none transition placeholder-[#6B7280] focus:border-[#FF8C00]"
                  />
                </div>

                {/* Concept list */}
                <div className="min-h-[200px] flex-1 divide-y divide-[#22222E] overflow-y-auto rounded-xl border border-[#22222E] bg-[#14141C]">
                  {searchResults.length > 0 ? (
                    searchResults.map((r) => (
                      <div
                        key={r.concept_id + (r.title || "")}
                        onClick={() => {
                          setSelectedConceptId(r.concept_id);
                          setSearchQuery("");
                        }}
                        className="cursor-pointer p-3 transition hover:bg-[#1E1E2A]"
                      >
                        <div className="text-xs font-semibold text-[#FF8C00]">{r.title}</div>
                        <div className="mt-0.5 text-[11px] text-[#9CA3AF]">
                          {r.domain_name} &gt; {r.category_name}
                        </div>
                      </div>
                    ))
                  ) : concepts.length === 0 ? (
                    <div className="p-4 text-center text-xs text-[#6B7280]">
                      Loading concepts…
                    </div>
                  ) : (
                    concepts.map((c) => {
                      const isSelected = c.id === selectedConceptId;
                      return (
                        <div
                          key={c.id}
                          onClick={() => setSelectedConceptId(c.id)}
                          className={`cursor-pointer border-l-2 p-3.5 transition ${
                            isSelected
                              ? "border-[#FF8C00] bg-[#FF8C00]/10"
                              : "border-transparent hover:bg-[#1E1E2A]"
                          }`}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <span
                              className={`text-xs font-semibold ${
                                isSelected ? "text-[#FF8C00]" : "text-white"
                              }`}
                            >
                              {c.name}
                            </span>
                            <span className="rounded bg-[#0A0A0F] px-1.5 py-0.5 font-mono text-[10px] uppercase text-[#6B7280]">
                              {c.difficulty_level}
                            </span>
                          </div>
                          <div className="mt-1 line-clamp-1 text-[11px] text-[#9CA3AF]">
                            {c.summary_definition}
                          </div>
                          <div className="mt-1.5 flex items-center gap-1 text-[10px] text-[#6B7280]">
                            <span>{c.domain_name}</span>
                            <span>•</span>
                            <span>{c.category_name}</span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>

            {/* Right: concept inspector */}
            <div className="lg:col-span-8">
              {loadingDetail ? (
                <div className="rounded-xl border border-[#22222E] bg-[#14141C] p-8 text-center text-xs text-[#9CA3AF]">
                  Loading concept details from SQLite…
                </div>
              ) : conceptDetail ? (
                <div className="space-y-6 rounded-xl border border-[#22222E] bg-[#14141C] p-6">
                  {/* Canonical header */}
                  <div>
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <Pill tone="orange">{conceptDetail.domain_name}</Pill>
                      <Pill>{conceptDetail.category_name}</Pill>
                      <Pill>{String(conceptDetail.difficulty_level || "").toUpperCase()}</Pill>
                      <Pill tone="green">
                        Confidence: {(conceptDetail.confidence_score * 100).toFixed(0)}%
                      </Pill>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <h2 className="text-xl font-bold text-white sm:text-2xl">
                        {conceptDetail.concept_name}
                      </h2>
                      <div className="flex items-center gap-2">
                        <code className="rounded-lg border border-[#2A2A36] bg-[#0A0A0F] px-2.5 py-1.5 font-mono text-xs text-[#9CA3AF]">
                          {conceptDetail.concept_id}
                        </code>
                        <CopyButton text={String(conceptDetail.concept_id ?? "")} label="Copy ID" />
                      </div>
                    </div>
                    <div className="mt-3 rounded-lg border border-[#22222E] bg-[#1E1E2A] p-3 text-xs font-medium leading-relaxed text-[#E5E7EB] sm:text-sm">
                      <strong className="text-white">Summary:</strong>{" "}
                      {conceptDetail.summary_definition}
                    </div>
                  </div>

                  {/* Primary explanation */}
                  <div>
                    <h3 className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                      Comprehensive Explanation
                    </h3>
                    <p className="rounded-lg border border-[#22222E] bg-[#0A0A0F] p-4 text-xs leading-relaxed text-[#D1D5DB] sm:text-sm">
                      {conceptDetail.primary_explanation}
                    </p>
                  </div>

                  {/* Formula / Key Properties */}
                  {(() => {
                    const keyProps = parseJsonArray<{ property: string; value: string }>(
                      conceptDetail.key_properties_json
                    );
                    const formulaRecords = (conceptDetail.records ?? []).filter((r: any) =>
                      ["formula", "theorem", "law"].includes(String(r.record_type))
                    );
                    if (keyProps.length === 0 && formulaRecords.length === 0) return null;
                    return (
                      <div>
                        <h3 className="mb-2 flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-[#FFC107]">
                          <Sigma className="h-3.5 w-3.5" />
                          Formula &amp; Key Properties
                        </h3>
                        <div className="space-y-3">
                          {formulaRecords.map((r: any) => (
                            <div
                              key={r.id ?? r.title}
                              className="rounded-lg border border-[#FFC107]/30 bg-[#FFC107]/5 p-3.5"
                            >
                              <div className="text-xs font-semibold text-[#FFC107]">{r.title}</div>
                              <code className="mt-1.5 block whitespace-pre-wrap font-mono text-xs leading-relaxed text-[#34D399]">
                                {r.content}
                              </code>
                            </div>
                          ))}
                          {keyProps.length > 0 && (
                            <div className="overflow-hidden rounded-lg border border-[#22222E] bg-[#0A0A0F]">
                              {keyProps.map((kp, i) => (
                                <div
                                  key={kp.property + i}
                                  className={`flex flex-col gap-1 p-3 sm:flex-row sm:items-baseline sm:gap-3 ${
                                    i > 0 ? "border-t border-[#22222E]" : ""
                                  }`}
                                >
                                  <span className="w-48 shrink-0 text-xs font-semibold text-white">
                                    {kp.property}
                                  </span>
                                  <span className="font-mono text-xs text-[#FFC107]">
                                    {kp.value}
                                  </span>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })()}

                  {/* Myth vs Reality */}
                  {conceptDetail.misconceptions &&
                    conceptDetail.misconceptions.length > 0 && (
                      <div>
                        <h3 className="mb-2 flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-[#FFC107]">
                          <AlertTriangle className="h-3.5 w-3.5" />
                          Myth vs. Reality — Common Misconceptions
                        </h3>
                        <div className="space-y-3">
                          {conceptDetail.misconceptions.map((m: any, i: number) => (
                            <div
                              key={i}
                              className="space-y-1.5 rounded-lg border border-amber-900/50 bg-amber-950/20 p-3.5 text-xs"
                            >
                              <div className="font-semibold text-red-400">
                                ❌ Myth: {m.misconception_text}
                              </div>
                              <div className="font-medium text-emerald-400">
                                ✓ Reality: {m.correction_text}
                              </div>
                              <div className="border-t border-amber-900/40 pt-1 text-[11px] text-[#9CA3AF]">
                                {m.explanation}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                  {/* Graph connections: prerequisites / incoming / outgoing */}
                  {(() => {
                    const outgoing: any[] = conceptDetail.relationships ?? [];
                    const incoming: any[] = conceptDetail.incoming_relationships ?? [];
                    const prereqIds: string[] = parseJsonArray<string>(
                      conceptDetail.prerequisites_json
                    );
                    if (
                      outgoing.length === 0 &&
                      incoming.length === 0 &&
                      prereqIds.length === 0
                    )
                      return null;

                    const tagClass =
                      "cursor-pointer rounded-lg border border-[#2A2A36] bg-[#1E1E2A] px-2.5 py-1.5 text-xs transition hover:border-[#FF8C00] hover:text-[#FF8C00]";

                    return (
                      <div>
                        <h3 className="mb-2 flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                          <Share2 className="h-3.5 w-3.5" />
                          Knowledge Graph Connections
                        </h3>
                        <div className="space-y-3">
                          {prereqIds.length > 0 && (
                            <div>
                              <div className="mb-1.5 text-[11px] font-semibold text-[#FF8C00]">
                                Prerequisites
                              </div>
                              <div className="flex flex-wrap gap-2">
                                {prereqIds.map((pid) => (
                                  <button
                                    key={pid}
                                    type="button"
                                    onClick={() => jumpToConcept(pid)}
                                    className={tagClass}
                                  >
                                    {conceptIndex[pid] ?? pid}
                                  </button>
                                ))}
                              </div>
                            </div>
                          )}
                          {incoming.length > 0 && (
                            <div>
                              <div className="mb-1.5 text-[11px] font-semibold text-[#FFC107]">
                                Incoming
                              </div>
                              <div className="flex flex-wrap gap-2">
                                {incoming.map((rel, i) => (
                                  <button
                                    key={`in-${i}`}
                                    type="button"
                                    onClick={() => jumpToConcept(rel.source_id)}
                                    className={tagClass}
                                    title={rel.description ?? ""}
                                  >
                                    {rel.source_name}
                                    <span className="ml-1.5 rounded bg-[#0A0A0F] px-1 py-0.5 font-mono text-[10px] text-[#6B7280]">
                                      {rel.relationship_type}
                                    </span>
                                  </button>
                                ))}
                              </div>
                            </div>
                          )}
                          {outgoing.length > 0 && (
                            <div>
                              <div className="mb-1.5 text-[11px] font-semibold text-[#9CA3AF]">
                                Outgoing
                              </div>
                              <div className="flex flex-wrap gap-2">
                                {outgoing.map((rel, i) => (
                                  <button
                                    key={`out-${i}`}
                                    type="button"
                                    onClick={() => jumpToConcept(rel.target_id)}
                                    className={tagClass}
                                    title={rel.description ?? ""}
                                  >
                                    {rel.target_name}
                                    <span className="ml-1.5 rounded bg-[#0A0A0F] px-1 py-0.5 font-mono text-[10px] text-[#6B7280]">
                                      {rel.relationship_type}
                                    </span>
                                  </button>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })()}

                  {/* RAG chunks viewer with token budget chips */}
                  {conceptDetail.chunks && conceptDetail.chunks.length > 0 && (
                    <div>
                      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                        <h3 className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                          <Cpu className="h-3.5 w-3.5" />
                          Pre-computed Mobile RAG Chunks
                        </h3>
                        <div className="flex flex-wrap items-center gap-1.5">
                          <Pill tone="orange">
                            Budget:{" "}
                            {conceptDetail.chunks.reduce(
                              (sum: number, ch: any) => sum + (ch.token_estimate ?? 0),
                              0
                            )}{" "}
                            tokens
                          </Pill>
                          <Pill>
                            {conceptDetail.chunks.length} chunks
                          </Pill>
                        </div>
                      </div>
                      <div className="space-y-2">
                        {conceptDetail.chunks.map((ch: any) => (
                          <div
                            key={ch.chunk_index}
                            className="rounded-lg border border-[#22222E] bg-[#0A0A0F] p-3 text-xs"
                          >
                            <div className="mb-1 flex flex-wrap items-center justify-between gap-2 text-[11px]">
                              <span className="font-mono text-[#FF8C00]">
                                Chunk #{ch.chunk_index} ({ch.chunk_type})
                              </span>
                              <span className="flex items-center gap-1.5">
                                <Pill tone="yellow">~{ch.token_estimate} tk</Pill>
                                <Pill>{ch.token_estimate ?? 0}/budget</Pill>
                              </span>
                            </div>
                            <p className="leading-relaxed text-[#D1D5DB]">{ch.text_content}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Source provenance */}
                  {conceptDetail.source_title && (
                    <div className="flex flex-wrap items-center justify-between gap-2 border-t border-[#22222E] pt-3 text-[11px] text-[#9CA3AF]">
                      <span>
                        Source: <strong className="text-white">{conceptDetail.source_title}</strong>{" "}
                        ({conceptDetail.source_type})
                      </span>
                      <span className="font-mono text-[#6B7280]">
                        Hash: {String(conceptDetail.content_hash ?? "").slice(0, 10)}…
                      </span>
                    </div>
                  )}
                </div>
              ) : (
                <div className="rounded-xl border border-[#22222E] bg-[#14141C] p-8 text-center text-xs text-[#6B7280]">
                  Select a concept from the list to inspect it.
                </div>
              )}
            </div>
          </div>
        )}

        {/* ---------------- TAB 3: RAG SIMULATOR ---------------- */}
        {activeTab === "rag" && (
          <div className="space-y-6">
            <Card className="space-y-4 p-6">
              <div>
                <h2 className="flex items-center gap-2 text-lg font-bold text-white">
                  <Sparkles className="h-5 w-5 text-[#FF8C00]" />
                  Kuzmix RAG Simulator &amp; Prompt Context Builder
                </h2>
                <p className="mt-1 text-xs text-[#9CA3AF]">
                  Test how the on-device engine retrieves exact knowledge chunks from SQLite and
                  builds the factual prompt context for local LLM generation.
                </p>
              </div>

              {/* Preset queries */}
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <span className="text-[#6B7280]">Try preset:</span>
                {[
                  "How does gravity behave in orbit?",
                  "Why pat meat dry before searing?",
                  "What is Big O of hash tables?",
                  "How to perform adult CPR?",
                  "What is separation of powers?",
                  "Difference between stiffness and strength"
                ].map((preset, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => {
                      setRagQuery(preset);
                      executeRagQuery(preset);
                    }}
                    className="h-8 rounded-full border border-[#2A2A36] bg-[#1E1E2A] px-3 text-[11px] text-[#D1D5DB] transition hover:border-[#FF8C00] hover:text-[#FF8C00]"
                  >
                    {preset}
                  </button>
                ))}
              </div>

              {/* Query + token slider */}
              <div className="grid grid-cols-1 gap-3 pt-2 md:grid-cols-12">
                <div className="flex gap-2 md:col-span-8">
                  <input
                    type="text"
                    value={ragQuery}
                    onChange={(e) => setRagQuery(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && executeRagQuery()}
                    placeholder="Enter a factual question or keywords…"
                    className="h-11 flex-1 rounded-lg border border-[#2A2A36] bg-[#0A0A0F] px-3.5 text-xs text-white outline-none transition placeholder-[#6B7280] focus:border-[#FF8C00]"
                  />
                  <button
                    type="button"
                    onClick={() => executeRagQuery()}
                    disabled={ragLoading || !ragQuery.trim()}
                    className="inline-flex h-11 items-center gap-2 rounded-lg bg-[#FF8C00] px-4 text-xs font-semibold text-[#0A0A0F] transition hover:bg-[#FF6A00] active:scale-95 disabled:opacity-50"
                  >
                    {ragLoading ? (
                      <>
                        <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-[#0A0A0F]/30 border-t-[#0A0A0F]" />
                        Retrieving…
                      </>
                    ) : (
                      "Simulate Retrieval"
                    )}
                  </button>
                </div>

                {/* Token cap slider + live estimate indicator */}
                <div className="flex flex-col gap-1.5 rounded-lg border border-[#2A2A36] bg-[#0A0A0F] px-3 py-2 md:col-span-4">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-[#9CA3AF]">Token Cap</span>
                    <span className="font-mono text-[#FF8C00]">
                      {ragTokenLimit} tk
                      {ragResult && (
                        <span className="ml-1.5 text-[#34D399]">
                          · ~{ragResult.estimatedTokens} used
                        </span>
                      )}
                    </span>
                  </div>
                  <input
                    type="range"
                    min="150"
                    max="600"
                    step="50"
                    value={ragTokenLimit}
                    onChange={(e) => setRagTokenLimit(parseInt(e.target.value, 10))}
                    className="h-1.5 w-full cursor-pointer appearance-none rounded-full bg-[#22222E] accent-[#FF8C00]"
                    style={{
                      background: `linear-gradient(to right, #FF8C00 ${
                        ((ragTokenLimit - 150) / 450) * 100
                      }%, #22222E ${((ragTokenLimit - 150) / 450) * 100}%)`
                    }}
                  />
                  {/* Live budget bar */}
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-[#22222E]">
                    <div
                      className={`h-full rounded-full transition-all ${
                        ragResult && ragResult.estimatedTokens >= ragTokenLimit * 0.9
                          ? "bg-[#FFC107]"
                          : "bg-[#34D399]"
                      }`}
                      style={{
                        width: ragResult
                          ? `${Math.min(100, Math.round((ragResult.estimatedTokens / ragTokenLimit) * 100))}%`
                          : "0%"
                      }}
                    />
                  </div>
                  <div className="flex justify-between font-mono text-[10px] text-[#6B7280]">
                    <span>150</span>
                    <span>
                      {ragResult
                        ? `${Math.round((ragResult.estimatedTokens / ragTokenLimit) * 100)}% of budget`
                        : "live estimate after run"}
                    </span>
                    <span>600</span>
                  </div>
                </div>
              </div>

              {ragResult?.disambiguationNotice && (
                <div className="rounded-lg border border-amber-800/50 bg-amber-950/30 p-3 text-xs text-amber-300">
                  {ragResult.disambiguationNotice}
                </div>
              )}
            </Card>

            {/* RAG results split */}
            {ragResult && (
              <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
                {/* Left: retrieved chunks + BM25 */}
                <div className="space-y-3 lg:col-span-5">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                      Retrieved Chunks ({ragResult.retrievedChunksCount})
                    </h3>
                    <Pill tone="orange">~{ragResult.estimatedTokens} tokens</Pill>
                  </div>
                  <div className="space-y-3">
                    {ragResult.chunks.map((ch: any, i: number) => (
                      <div key={i} className="space-y-1.5 rounded-xl border border-[#22222E] bg-[#14141C] p-3.5 text-xs">
                        <div className="flex flex-wrap items-center justify-between gap-2 text-[11px]">
                          <span className="font-semibold text-[#FF8C00]">Fact #{i + 1}</span>
                          <span className="flex items-center gap-1.5">
                            <Pill tone="yellow">
                              BM25{" "}
                              {typeof ch.bm25_score === "number"
                                ? ch.bm25_score.toFixed(3)
                                : "—"}
                            </Pill>
                            <Pill>~{ch.token_estimate} tk</Pill>
                          </span>
                        </div>
                        <div className="font-mono text-[10px] text-[#6B7280]">
                          {ch.header_context}
                        </div>
                        <p className="rounded border border-[#22222E] bg-[#0A0A0F] p-2.5 leading-relaxed text-[#D1D5DB]">
                          {ch.text_content}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Right: synthesized grounded context block */}
                <div className="space-y-2 lg:col-span-7">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                      Synthesized Grounded Context
                    </h3>
                    <CopyButton text={ragResult.formattedContext ?? ""} label="Copy Context" />
                  </div>
                  <div className="overflow-hidden rounded-xl border border-[#22222E] bg-[#14141C]">
                    <div className="flex items-center justify-between border-b border-[#22222E] bg-[#1C1C26] px-4 py-2 font-mono text-[11px] text-[#FFC107]">
                      <span>// KUZMIX OFFLINE GROUNDED CONTEXT BLOCK</span>
                      <span className="text-[#6B7280]">ready to paste</span>
                    </div>
                    <pre className="max-h-[500px] overflow-auto whitespace-pre-wrap bg-[#0A0A0F] p-4 font-mono text-xs leading-relaxed text-[#34D399]">
                      {ragResult.formattedContext}
                    </pre>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ---------------- TAB 4: ANDROID CONTRACT ---------------- */}
        {activeTab === "android" && <AndroidContractTab />}

        {/* ---------------- TAB 5: TESTS & INTEGRITY ---------------- */}
        {activeTab === "tests" && (
          <div className="space-y-6">
            <div className="flex flex-col gap-4 rounded-xl border border-[#22222E] bg-[#14141C] p-6 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="flex items-center gap-2 text-lg font-bold text-white">
                  <FlaskConical className="h-5 w-5 text-emerald-400" />
                  Automated Database &amp; Integrity Test Suite
                </h2>
                <p className="mt-1 text-xs text-[#9CA3AF]">
                  14 automated test points verifying B-Tree integrity, foreign keys,
                  deduplication, FTS5 BM25 search, cascade deletes and Android query contracts.
                </p>
              </div>
              <button
                type="button"
                onClick={handleRunTests}
                disabled={runningTests}
                className="inline-flex h-11 items-center gap-2 rounded-lg bg-[#FF8C00] px-5 text-xs font-semibold text-[#0A0A0F] transition hover:bg-[#FF6A00] active:scale-95 disabled:opacity-50"
              >
                {runningTests ? (
                  <>
                    <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-[#0A0A0F]/30 border-t-[#0A0A0F]" />
                    Running Tests…
                  </>
                ) : (
                  <>
                    <Play className="h-4 w-4" />
                    Run All 14 Automated Tests
                  </>
                )}
              </button>
            </div>

            {/* Result banner */}
            {testResult && (
              <div
                className={`flex flex-wrap items-center gap-2 rounded-xl border p-4 text-xs ${
                  testResult.success
                    ? "border-emerald-800/60 bg-emerald-950/40 text-emerald-300"
                    : "border-red-800/70 bg-red-950/40 text-red-300"
                }`}
              >
                {testResult.success ? (
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
                ) : (
                  <XCircle className="h-4 w-4 shrink-0 text-red-400" />
                )}
                <span>
                  {testResult.message || testResult.error || "Test run finished."}
                </span>
                {testResult.success && testResult.passed != null && (
                  <span className="ml-auto flex items-center gap-3 font-mono">
                    <Pill tone="green">
                      {testResult.passed}/{testResult.total} passed
                    </Pill>
                    <Pill tone="orange">{testResult.durationMs} ms</Pill>
                  </span>
                )}
              </div>
            )}

            {/* Status matrix */}
            <div className="overflow-hidden rounded-xl border border-[#22222E] bg-[#14141C]">
              <div className="flex items-center justify-between border-b border-[#22222E] bg-[#1C1C26] px-4 py-2.5">
                <span className="text-xs font-semibold text-white">Test Status Matrix</span>
                <span className="font-mono text-[11px] text-[#6B7280]">
                  {testResult?.tests ? `${testResult.tests.length} results` : "not run yet"}
                </span>
              </div>
              <div className="divide-y divide-[#22222E]">
                {(() => {
                  const results: TestCaseView[] = Array.isArray(testResult?.tests)
                    ? testResult.tests
                    : [];
                  const byName = new Map(results.map((r) => [r.name, r]));
                  return TEST_CATALOG.map((t, idx) => {
                    const result = byName.get(t.name);
                    const status = result?.status;
                    return (
                      <div key={idx} className="flex items-start gap-3 px-4 py-3">
                        <span className="mt-0.5 shrink-0">
                          {status === "passed" ? (
                            <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                          ) : status === "failed" ? (
                            <XCircle className="h-4 w-4 text-red-400" />
                          ) : (
                            <CircleDashed className="h-4 w-4 text-[#6B7280]" />
                          )}
                        </span>
                        <div className="min-w-0 flex-1">
                          <div className="text-xs font-semibold text-white">{t.name}</div>
                          <div className="mt-0.5 text-[11px] text-[#9CA3AF]">{t.desc}</div>
                        </div>
                        <div className="flex shrink-0 items-center gap-2">
                          {result?.durationMs != null && (
                            <span className="flex items-center gap-1 font-mono text-[10px] text-[#6B7280]">
                              <Clock className="h-3 w-3" />
                              {result.durationMs} ms
                            </span>
                          )}
                          <span
                            className={`rounded border px-2 py-0.5 font-mono text-[10px] ${
                              status === "passed"
                                ? "border-emerald-800/60 bg-emerald-950/50 text-emerald-400"
                                : status === "failed"
                                  ? "border-red-800/70 bg-red-950/50 text-red-400"
                                  : "border-[#2A2A36] bg-[#1E1E2A] text-[#6B7280]"
                            }`}
                          >
                            {status === "passed"
                              ? "PASS"
                              : status === "failed"
                                ? "FAIL"
                                : "PENDING"}
                          </span>
                        </div>
                      </div>
                    );
                  });
                })()}
              </div>
            </div>

            {/* Integrity report */}
            <Card className="p-6">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-bold text-white">Integrity Report</h3>
                  <p className="text-xs text-[#9CA3AF]">
                    Full SQLite validation: schema, foreign keys, orphans, FTS coverage and
                    content quality.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleValidate}
                  disabled={validating}
                  className="inline-flex h-10 items-center gap-2 rounded-lg border border-[#2A2A36] bg-[#1E1E2A] px-4 text-xs font-semibold text-white transition hover:border-[#FF8C00] active:scale-95 disabled:opacity-50"
                >
                  {validating ? "Validating…" : "Refresh Integrity Check"}
                </button>
              </div>

              {!integrity ? (
                <div className="rounded-lg border border-[#22222E] bg-[#0A0A0F] p-4 text-center text-xs text-[#6B7280]">
                  Run the test suite (or click refresh) to load the integrity report.
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="flex flex-wrap items-center gap-2">
                    <Pill tone={integrity.passed ? "green" : "red"}>
                      {integrity.passed ? "✓ PASSED — all checks clear" : "✗ FAILED"}
                    </Pill>
                    <Pill tone="neutral">
                      {integrity.errors?.length ?? 0} errors · {integrity.warnings?.length ?? 0}{" "}
                      warnings
                    </Pill>
                  </div>

                  {integrity.errors?.length > 0 && (
                    <div className="space-y-1 rounded-lg border border-red-800/60 bg-red-950/30 p-3">
                      {integrity.errors.map((e: string, i: number) => (
                        <div key={i} className="text-xs text-red-300">
                          ✗ {e}
                        </div>
                      ))}
                    </div>
                  )}
                  {integrity.warnings?.length > 0 && (
                    <div className="space-y-1 rounded-lg border border-amber-800/50 bg-amber-950/20 p-3">
                      {integrity.warnings.map((w: string, i: number) => (
                        <div key={i} className="text-xs text-amber-300">
                          ! {w}
                        </div>
                      ))}
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
                    {Object.entries(integrity.metrics ?? {}).map(([key, val]) => (
                      <div
                        key={key}
                        className="rounded-lg border border-[#22222E] bg-[#0A0A0F] p-3 text-center"
                      >
                        <div className="text-sm font-bold text-white">
                          {typeof val === "number" ? val.toLocaleString() : String(val)}
                        </div>
                        <div className="mt-0.5 font-mono text-[10px] text-[#6B7280]">{key}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </Card>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-[#22222E] bg-[#14141C]/60 py-4 text-center text-xs text-[#6B7280]">
        Kuzmix AI Offline Knowledge Core · Knowledge Core v{schemaVersion} · SQLite · FTS5 ·
        Android Production Artifact · by The Kreadive Galaxy
      </footer>

      {/* Auth modal */}
      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} />
    </div>
  );
}

/* Keeps unused-import parity guards honest for optional helper exports. */
export type { StatsData, DomainItem, ConceptSummary };
