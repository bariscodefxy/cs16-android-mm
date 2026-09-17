# Product Context — Nexora

## Why this exists
Stock CS 1.6 Android clients ship no server-side-style plugin system. Players
want AMX Mod X plugins (admin, fun, stats, bots) inside the mobile client
without rebuilding the game from source twice. Nexora delivers that as a
one-tap APK patch on the device itself, offline-capable.

## Problems it solves
- No AMXX/metamod support in the mobile CS 1.6 client out of the box.
- 32-bit-cell AMXX cannot run on arm64 Android (pointer truncation) — needs a
  full 64-bit-cell toolchain port (core + modules + compiler + plugins).
- Users have XP-era `.sma` plugin sources; they need them compiled to matching
  64-bit `.amxx` without a PC.
- Updates must be small: full re-download per release is wasteful → per-`.so`
  SHA-256 incremental manifest.

## How it should work (UX)
1. **Patch tab** — select CS 1.6 `.apk` → component checklist popup (all
   checked; uncheck to skip) → Patch → install output APK. Identity/signature
   kept so account + game data survive.
2. **Compile tab** — drop `.sma` into `addons/amxmodx/scripting/` → Nexora
   compiles to `.amxx` (logs: `scripting/logs/compiler.log`, `error.log`) →
   included at next Patch.
3. **Addons tab** — embedded AMXX package works offline; auto-install + status
   scan on launch (`autoInstallAddons()`, `scanAddonsStatus()`).
4. Overflow — Plugins list, Releases, CrashLog view, log share, app
   self-update (15 s GitHub poll while open), bundle redownload, About.

## UX goals
- Never brick the game APK: validate ABI (`arm64-v8a` only for full patch),
  report removed/added/kept entries, alignment, signature verification.
- Offline-first: bundle embedded in app assets as fallback; newest release as
  primary source.
- Failures must be explainable: structured `PatchReport` (sizes, counts,
  `moduleLibs`, `patchedLibs`, verification) animates the UI and is shareable.
