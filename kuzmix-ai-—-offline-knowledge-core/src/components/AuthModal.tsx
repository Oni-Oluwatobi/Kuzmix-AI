import { useEffect, useState, type FormEvent } from "react";
import { ShieldCheck, X, Mail, Lock, KeyRound } from "lucide-react";
import { useAuth } from "../context/AuthContext";

type AuthMode = "signin" | "signup";

const FIREBASE_ERRORS: Record<string, string> = {
  "auth/invalid-email": "That email address looks invalid. Please check it and try again.",
  "auth/missing-email": "Please enter your email address.",
  "auth/missing-password": "Please enter your password.",
  "auth/weak-password": "Password must be at least 6 characters long.",
  "auth/email-already-in-use": "An account with this email already exists. Use Sign In instead.",
  "auth/invalid-credential": "Wrong email or password. Please try again.",
  "auth/wrong-password": "Wrong email or password. Please try again.",
  "auth/user-not-found": "No account found for this email. Create one first.",
  "auth/too-many-requests": "Too many attempts. Please wait a moment and try again.",
  "auth/network-request-failed": "Network error. Check your connection and retry.",
  "auth/unauthorized-domain": "This domain is not authorized in Firebase. Add it under Authentication → Settings → Authorized domains.",
  "auth/internal-error": "Firebase returned an internal error. Please retry.",
  "auth/operation-not-allowed": "Email/Password sign-in is disabled in the Firebase console."
};

function friendlyError(err: unknown): string {
  const code =
    typeof err === "object" && err !== null && "code" in err
      ? String((err as { code?: unknown }).code)
      : "";
  if (code && FIREBASE_ERRORS[code]) return FIREBASE_ERRORS[code];
  const message =
    typeof err === "object" && err !== null && "message" in err
      ? String((err as { message?: unknown }).message)
      : "";
  return message.replace(/^Firebase:\s*/i, "") || "Something went wrong. Please try again.";
}

function Spinner({ dark = true }: { dark?: boolean }) {
  return (
    <span
      className={`inline-block h-4 w-4 animate-spin rounded-full border-2 ${
        dark ? "border-[#0A0A0F]/30 border-t-[#0A0A0F]" : "border-white/30 border-t-white"
      }`}
      aria-hidden="true"
    />
  );
}

export default function AuthModal({
  open,
  onClose
}: {
  open: boolean;
  onClose: () => void;
}) {
  const { currentUser, login, signup } = useAuth();
  const [mode, setMode] = useState<AuthMode>("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Close automatically once authentication succeeds.
  useEffect(() => {
    if (open && currentUser) onClose();
  }, [open, currentUser, onClose]);

  // Reset transient state whenever the modal closes.
  useEffect(() => {
    if (!open) {
      setError(null);
      setBusy(false);
    }
  }, [open]);

  // Escape closes the modal (unless an auth query is in flight).
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !busy) onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, busy, onClose]);

  if (!open) return null;

  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  const passwordValid =
    mode === "signin" ? password.length >= 1 : password.length >= 6;
  const confirmValid = mode === "signin" ? true : confirm === password && confirm.length > 0;
  const canSubmit = emailValid && passwordValid && confirmValid && !busy;

  const switchMode = (next: AuthMode) => {
    if (busy) return;
    setMode(next);
    setError(null);
    setConfirm("");
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;
    setBusy(true);
    setError(null);
    try {
      if (mode === "signin") {
        await login(email, password);
      } else {
        if (confirm !== password) {
          setError("Passwords do not match.");
          return;
        }
        await signup(email, password);
      }
      setPassword("");
      setConfirm("");
      onClose();
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setBusy(false);
    }
  };

  const inputClass =
    "h-12 w-full rounded-lg border border-[#2A2A36] bg-[#0A0A0F] px-3.5 text-sm text-white placeholder-[#6B7280] outline-none transition focus:border-[#FF8C00] disabled:opacity-60";

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busy) onClose();
      }}
      role="dialog"
      aria-modal="true"
      aria-label="Authentication"
    >
      <div className="w-full max-w-md rounded-2xl border border-[#2A2A36] bg-[#14141C] p-6 shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#FF8C00]">
              <ShieldCheck className="h-5 w-5 text-[#0A0A0F]" />
            </div>
            <div>
              <h2 className="text-lg font-bold tracking-tight text-white">
                {mode === "signin" ? "Welcome back" : "Create your account"}
              </h2>
              <p className="text-xs text-[#9CA3AF]">
                Kuzmix AI Knowledge Core · by The Kreadive Galaxy
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            aria-label="Close"
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-[#2A2A36] text-[#9CA3AF] transition hover:border-[#FF8C00] hover:text-white disabled:opacity-50"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Mode tabs */}
        <div className="mt-5 grid grid-cols-2 gap-1 rounded-lg border border-[#22222E] bg-[#0A0A0F] p-1">
          {(
            [
              { id: "signin", label: "Sign In" },
              { id: "signup", label: "Create Account" }
            ] as { id: AuthMode; label: string }[]
          ).map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => switchMode(tab.id)}
              disabled={busy}
              className={`h-10 rounded-md text-xs font-semibold transition ${
                mode === tab.id
                  ? "bg-[#1E1E2A] text-[#FF8C00]"
                  : "text-[#9CA3AF] hover:text-white"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <form onSubmit={submit} className="mt-5 space-y-4">
          {/* Email */}
          <div>
            <label
              htmlFor="auth-email"
              className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-[#9CA3AF]"
            >
              Email
            </label>
            <div className="relative">
              <Mail className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-[#6B7280]" />
              <input
                id="auth-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                disabled={busy}
                className={`${inputClass} pl-9`}
              />
            </div>
          </div>

          {/* Password */}
          <div>
            <label
              htmlFor="auth-password"
              className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-[#9CA3AF]"
            >
              Password
            </label>
            <div className="relative">
              <Lock className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-[#6B7280]" />
              <input
                id="auth-password"
                type="password"
                autoComplete={mode === "signin" ? "current-password" : "new-password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={mode === "signin" ? "Your password" : "At least 6 characters"}
                disabled={busy}
                className={`${inputClass} pl-9`}
              />
            </div>
          </div>

          {/* Confirm password (sign up only) */}
          {mode === "signup" && (
            <div>
              <label
                htmlFor="auth-confirm"
                className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-[#9CA3AF]"
              >
                Confirm Password
              </label>
              <div className="relative">
                <KeyRound className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-[#6B7280]" />
                <input
                  id="auth-confirm"
                  type="password"
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="Repeat your password"
                  disabled={busy}
                  className={`${inputClass} pl-9`}
                />
              </div>
              {confirm.length > 0 && confirm !== password && (
                <p className="mt-1.5 text-xs text-[#F87171]">Passwords do not match.</p>
              )}
            </div>
          )}

          {/* Error message */}
          {error && (
            <div
              role="alert"
              className="rounded-lg border border-red-800/70 bg-red-950/40 px-3.5 py-2.5 text-xs font-medium leading-relaxed text-red-300"
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={!canSubmit}
            className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[#FF8C00] text-sm font-semibold text-[#0A0A0F] transition hover:bg-[#FF6A00] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {busy ? (
              <>
                <Spinner />
                <span>{mode === "signin" ? "Signing in..." : "Creating account..."}</span>
              </>
            ) : mode === "signin" ? (
              "Sign In"
            ) : (
              "Create Account"
            )}
          </button>
        </form>

        <p className="mt-4 text-center text-[11px] leading-relaxed text-[#6B7280]">
          Secured by Firebase Authentication · your session stays signed in across tabs
        </p>
      </div>
    </div>
  );
}
