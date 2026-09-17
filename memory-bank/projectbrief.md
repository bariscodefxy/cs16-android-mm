# Project Brief — Nexora

## What this is
Nexora (`applicationId com.nexora`, code namespace `com.pickle.patcher`) is an
Android APK patcher that injects AMX Mod X + Metamod + plugins into a stock
CS 1.6 client APK (Xash3D-based, `vcs16/` submodule). Pick APK → Patch →
install output. It also compiles `.sma` → `.amxx` on-device.

## Core requirements
1. AMXX must run as a **64-bit-cell** build (`PAWN_CELL_SIZE=64`) on
   **aarch64 Android** — a 32-bit cell cannot hold a function pointer in
   `amx_BrowseRelocate`. Core, all modules, and the compiler must agree on
   cell size or every plugin is rejected ("section not found").
2. Upstream is never vendored: `alliedmodders/amxmodx@master`,
   `FWGS/metamod-fwgs`, `rehlds/ReAPI`, `yapb/yapb`, `berkchy/ReGameDLL_CS`
   are fetched fresh in CI; `android/hlsdk` (`alliedmodders/hlsdk`),
   `android/mm-p` (`Bots-United/metamod-p@master`) and `vcs16` are pinned
   submodules. Every local change lives in `patches/`, applied by
   `android/ci/build-amxx.sh` onto throwaway `$SRC` copies (submodules are
   never dirtied).
3. The patcher must never damage the game: prune ONLY exact AMXX/metamod lib
   paths + `META-INF/` (a past `lib/<abi>/lib` prefix match deleted the whole
   engine). `.so` entries STORED + aligned, `resources.arsc` STORED +
   4-byte aligned, output re-signed V1+V2 and verified.
4. `arm64-v8a` is the supported ABI (modules `*_amxx_amd64.so`); `armeabi-v7a`
   (`*_amxx_arm.so`) is best-effort trial and its failure must never fail CI.

## Scope boundaries
- In tree: patcher app + patch engine, CI build/packaging scripts, `patches/`,
  `android/hlsdk` + `android/mm-p` + `vcs16` submodules, `addons/` data tree.
- Out of tree: game APK itself, fetched upstream sources, signing keys,
  build outputs (`src/`, `build-out/`, `rgdll-*`).
- `addons/` on master is a placeholder; the real tree ships from the
  `amxx-addons` branch via `git checkout origin/amxx-addons -- addons/` in CI.
- `libmenu` is intentionally NOT shipped (broken text menu — stock stays).

## Source of truth order
`MEMORY_BANK.md` (protocol) → `memory-bank/` (this bank) → `patches/` (+
`patches/README.md` catalog) + CI scripts (ground truth for native) →
`CLAUDE.md` (workflow rules).
