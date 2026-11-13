import { useState, type ReactNode } from "react";
import { Check, Copy } from "lucide-react";

/* ------------------------------------------------------------------ */
/*  TKG Design System atoms (The Kreadive Galaxy)                      */
/*  Canvas #0A0A0F · Surface #14141C · Elevated #1C1C26                */
/*  Borders #22222E / #2A2A36 · Orange #FF8C00 · Yellow #FFC107        */
/* ------------------------------------------------------------------ */

export const TKG = {
  canvas: "#0A0A0F",
  surface: "#14141C",
  elevated: "#1C1C26",
  variant: "#1E1E2A",
  border: "#22222E",
  borderActive: "#2A2A36",
  orange: "#FF8C00",
  orangeDeep: "#FF6A00",
  yellow: "#FFC107",
  yellowDeep: "#FFD400",
  textPrimary: "#FFFFFF",
  textSecondary: "#9CA3AF",
  textMuted: "#6B7280"
} as const;

export function Card({
  children,
  className = "",
  hover = false
}: {
  children: ReactNode;
  className?: string;
  hover?: boolean;
}) {
  return (
    <div
      className={`rounded-xl border border-[#22222E] bg-[#14141C] transition ${
        hover ? "hover:-translate-y-[2px] hover:border-[#2A2A36]" : ""
      } ${className}`}
    >
      {children}
    </div>
  );
}

export function SectionTitle({
  children,
  hint,
  action
}: {
  children: ReactNode;
  hint?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
      <div className="flex items-center gap-2">
        <h2 className="text-sm font-bold tracking-tight text-white">{children}</h2>
        {hint && <span className="text-[11px] text-[#6B7280]">{hint}</span>}
      </div>
      {action}
    </div>
  );
}

export function Pill({
  children,
  tone = "neutral",
  className = ""
}: {
  children: ReactNode;
  tone?: "neutral" | "orange" | "yellow" | "green" | "red" | "amber";
  className?: string;
}) {
  const tones: Record<string, string> = {
    neutral: "border-[#2A2A36] bg-[#1E1E2A] text-[#9CA3AF]",
    orange: "border-[#FF8C00]/40 bg-[#FF8C00]/10 text-[#FF8C00]",
    yellow: "border-[#FFC107]/40 bg-[#FFC107]/10 text-[#FFC107]",
    green: "border-emerald-700/50 bg-emerald-950/50 text-emerald-400",
    red: "border-red-800/60 bg-red-950/40 text-red-300",
    amber: "border-amber-800/50 bg-amber-950/40 text-amber-300"
  };
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-md border px-2 py-0.5 font-mono text-[11px] ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

export function StatusDot({ tone = "green" }: { tone?: "green" | "orange" | "red" }) {
  const colors: Record<string, string> = {
    green: "bg-emerald-400",
    orange: "bg-[#FF8C00]",
    red: "bg-red-400"
  };
  return (
    <span className="relative inline-flex h-2 w-2">
      <span className={`absolute inline-flex h-full w-full animate-ping rounded-full opacity-60 ${colors[tone]}`} />
      <span className={`relative inline-flex h-2 w-2 rounded-full ${colors[tone]}`} />
    </span>
  );
}

export function CopyButton({
  text,
  label = "Copy",
  className = ""
}: {
  text: string;
  label?: string;
  className?: string;
}) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1600);
    } catch {
      setCopied(false);
    }
  };
  return (
    <button
      type="button"
      onClick={copy}
      className={`inline-flex h-8 items-center gap-1.5 rounded-md border px-2.5 text-[11px] font-semibold transition active:scale-95 ${
        copied
          ? "border-emerald-700/60 bg-emerald-950/50 text-emerald-400"
          : "border-[#2A2A36] bg-[#1E1E2A] text-[#9CA3AF] hover:border-[#FF8C00] hover:text-[#FF8C00]"
      } ${className}`}
      title={label}
    >
      {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
      {copied ? "Copied" : label}
    </button>
  );
}

export function MetricCard({
  label,
  value,
  hint
}: {
  label: string;
  value: ReactNode;
  hint?: string;
}) {
  return (
    <div className="flex flex-col justify-between rounded-xl border border-[#22222E] bg-[#14141C] p-4 transition hover:-translate-y-[2px] hover:border-[#2A2A36]">
      <div className="text-xs font-medium text-[#9CA3AF]">{label}</div>
      <div className="mt-1 text-xl font-bold text-white">{value}</div>
      {hint && <div className="mt-1 font-mono text-[11px] text-[#FF8C00]">{hint}</div>}
    </div>
  );
}

/* --------------------------- SQL highlighting ---------------------- */
/* Neutral / warm palette only — orange keywords, yellow functions,    */
/* green strings, muted comments. Strictly no blue.                    */

const SQL_KEYWORDS = new Set(
  ("select from where and or not null limit offset union all group by order asc desc join inner left " +
    "on as is in between exists case when then else end distinct with recursive using values set into " +
    "primary key foreign references cascade check default constraint index view create table if " +
    "insert delete update from pragma function match bm25 lower trim min max count sum avg substr " +
    "coalesce like glob cast interval collate").split(" ")
);

const SQL_TOKEN_RE =
  /('(?:[^']|'')*')|(--[^\n]*)|([A-Za-z_][A-Za-z_0-9]*)/g;

export function SqlHighlight({ code }: { code: string }) {
  const parts: ReactNode[] = [];
  let last = 0;
  let match: RegExpExecArray | null;
  let key = 0;
  SQL_TOKEN_RE.lastIndex = 0;
  while ((match = SQL_TOKEN_RE.exec(code)) !== null) {
    if (match.index > last) parts.push(code.slice(last, match.index));
    const [full, str, comment, word] = match;
    if (str) {
      parts.push(
        <span key={key++} className="text-[#34D399]">
          {full}
        </span>
      );
    } else if (comment) {
      parts.push(
        <span key={key++} className="italic text-[#6B7280]">
          {full}
        </span>
      );
    } else if (word) {
      const lower = word.toLowerCase();
      if (SQL_KEYWORDS.has(lower)) {
        parts.push(
          <span key={key++} className="font-semibold text-[#FF8C00]">
            {full}
          </span>
        );
      } else if (/^[A-Za-z_]/.test(word)) {
        parts.push(
          <span key={key++} className="text-[#D1D5DB]">
            {full}
          </span>
        );
      }
    }
    last = match.index + full.length;
  }
  if (last < code.length) parts.push(code.slice(last));
  return <>{parts}</>;
}

export function CodeBlock({
  title,
  badge,
  code,
  language = "sql"
}: {
  title: string;
  badge?: string;
  code: string;
  language?: "sql" | "kotlin" | "text";
}) {
  return (
    <div className="overflow-hidden rounded-xl border border-[#22222E] bg-[#14141C]">
      <div className="flex items-center justify-between gap-2 border-b border-[#22222E] bg-[#1C1C26] px-3.5 py-2">
        <div className="flex min-w-0 items-center gap-2">
          <span className="truncate font-mono text-xs font-semibold text-[#FF8C00]">{title}</span>
          {badge && (
            <span className="shrink-0 rounded border border-[#2A2A36] bg-[#0A0A0F] px-1.5 py-0.5 font-mono text-[10px] text-[#9CA3AF]">
              {badge}
            </span>
          )}
        </div>
        <CopyButton text={code} />
      </div>
      <pre className="overflow-x-auto bg-[#0A0A0F] p-4 font-mono text-xs leading-relaxed text-[#D1D5DB]">
        {language === "sql" ? <SqlHighlight code={code} /> : code}
      </pre>
    </div>
  );
}
