# System Patterns — Nexora

## Architecture
- `android/app` (Compose, `com.pickle.patcher`): `MainActivity` bottom-nav
  (Patch/Compile/Addons) + overflow (Plugins/Releases/CrashLog);
  `PatcherViewModel` orchestrates patch/compile/addons/update;
  `IncrementalUpdateManager` applies per-`.so` updates from `manifest.json`;
  `BundleProvider` / `ReleaseRepository` fetch embedded-asset or GitHub bundle.
- `android/patcherlib` (pure JVM, `com.pickle.patcher.lib`): `ApkPatcher.patch`
  runs ANALYZE → INJECT (`ZipRepacker.repack`) → ALIGN check → SIGN
  (`ApkSignerTool`, V1+V2) → VERIFY. `BundleManifest` (`bundle.json`) +
  `ExcludeRule.DEFAULT` define payload and prune set. `ZipRaw`/`ZipAnalyzer`
  do low-level ZIP parsing; `BundleBuilder` packs; `Cli` enables host runs.
- Native pipeline: `build-amxx.sh` (fetch → `git apply patches/` → NDK
  cross-compile → host+device pawncc → `.sma` compile) → `gen-manifest.py`
  (`manifest.json` + staged `.so`) + `amxx-addons.zip` → Release assets →
  patcher injects.

## Key technical decisions (migrated from CHANGES.md)
- **64-bit cells everywhere** (`-DPAWN_CELL_SIZE=64`): `amxmodx-64bit-cell-casts`
  (`ke::Max(0,…)` → `(cell)0`), `amxmodx-amtl-64bit` (`Min/Max<T1,T2>` with
  `decltype`), `amxmodx-CDetour-cell` (`cell_t32`/`cell_t64` via
  `#ifdef PAWN_CELL_SIZE`), `amxmodx-pawncc-64bit` (`Compile64` dlsym +
  `cellsize = sizeof(cell)` + drop `prefix.h` + `-DLINUX`),
  `amxmodx-memtools-dlfcn` (`dlfcn.h`), plus `param-convert`, `pcvar-handle`,
  `float64`, `fakemeta-intvec`, `cbase-bit32`, `ham-float64`, `pawncc-literalpool`,
  `libpawnc-litq`, `sc6-state-dbginfo`, `amx-hea-adopt`, `fun-strip-user-weapons`
  patches. ARM FTZ bit disabled in fakemeta so denormal vectors survive.
- **Android loading via dlopen**: `amxmodx-android-load-CModule` /
  `amxmodx-android-load-modules` (no `__android_log_print`, no mod-relative
  assumption; modules from `<gamedir>/addons/amxmodx/modules/`).
  `MemoryUtils` skips ELF fallback on Android (bionic `l_name` NULL → SIGSEGV).
  `CDetour::CreateDetour` returns false on ARM64 (x86 trampoline invalid).
  `Trampolines.h` uses `intptr_t`. Module suffix: arm64 `_amd64`, arm32 `_arm`.
- **Metamod**: core compiles against the `mm-p` submodule
  (`Bots-United/metamod-p@master`: `meta_api.h` + `metamod-p-aarch64.patch` +
  `metamod-p-meta-debug-developer.patch`, which stops `meta_debug 3`
  auto-enable in developer mode); runtime is `metamod-fwgs`
  (`metamod-fwgs-android.patch`, CMake → `libmetamod_android_*.so` → renamed
  `libmetamod.so`, injected as `libyapb_android_*.so` for `-dll @yapb`).
  `plugins.ini` chains `addons/amxmodx/libamxmodx.so` + YaPB.
- **HLSDK identity** (verified by SHA-256 file-hash comparison, Sep 2026):
  `android/hlsdk` is `alliedmodders/hlsdk` (Valve 2.3p3 mirror, AMXX's
  canonical SDK — 279/279 vendored files byte-identical), NOT
  `FWGS/hlsdk-portable` (214 files differ). The old repo-root FWGS `hlsdk`
  gitlink belonged to the removed `mm-fwgs` experiment and is gone.
- **ReGameDLL first-spawn fix** (`regamedll-spawn-justconnected.diff`,
  baseline `7be9d59`, fork branch `fix/first-spawn-equip`): first spawn while
  `m_bJustConnected` skipped `OnSpawnEquip`/`GiveDefaultItems` → weapon-less.
  Fix promotes in-team spawn to in-game spawn (`JOINED`, clears
  `m_bJustConnected`/`m_bNotKilled`); `PICKINGTEAM` no-op kept. Ships as
  `libcs_android_arm64.so`; patcher falls back to byte-patching base APK
  `libcs` when absent (`ZipRepacker.patchLibCs`: `cbz` → `tbz` arm64 fixup).

## Critical implementation paths
- `ApkPatcher.patch` → `ZipRepacker.repack(source, output, bundle, exclude,
  pruneAbiExcept)` → align-verify all STORED → `ApkSignerTool.sign` → verify.
  Any misaligned STORED entry throws.
- `build-amxx.sh <src> <ndk> <out> "" [abi]` (`plugins-src` dropped; the
  plugin-compile step self-skips when the dir is unset): fetch amxmodx +
  metamod-fwgs + reapi + yapb, copy mm-p submodule to `$SRC` (`.git`
  stripped, throwaway repo init), apply `patches/` + inline
  python/awk fixes, build core → pcre 8.45 → metamod → 12 AMXX modules →
  reapi → yapb → vcs16 client+menu → host pawncc → plugins → device amxxpc.
- `gen-manifest.py <abi> <libdir> <outdir>`: SHA-256 manifest, ABI-prefixed
  asset names (`arm64-v8a__*.so`) to avoid cross-ABI release collisions.
