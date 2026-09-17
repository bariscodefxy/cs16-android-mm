# patches/ — native customization index

Every native change lives here as a file. Nothing upstream is edited in
place (not submodules, not CI-fetched trees).

## How it works

- `android/ci/build-amxx.sh::apply_patch()` runs `git apply --check` then
  `git apply` on a **throwaway `$SRC` copy** (submodule content copied +
  `git init`). The `---`/`+++` paths are authoritative; ignore stale prefixes
  in `diff --git` header lines if you ever see local-machine paths there.
- Application order = the order of `apply_patch` lines in `build-amxx.sh`.
  One exception takes a subdir: `amxmodx-amtl-64bit.diff` applies inside
  `$SRC/amxmodx/public/amtl`.
- `apply_patch` is idempotent (`.applied-<name>` marker per target).
- `mainui-menu-text-and-trim.patch` is the odd one out: applied with plain
  `git apply` onto `vcs16/3rdparty/mainui_cpp`, tolerant (`|| WARN`).
- `regamedll-spawn-justconnected.diff` is **reference only** — the fix ships
  via the `berkchy/ReGameDLL_CS@fix/first-spawn-equip` fork branch cloned in
  the workflow, the script never applies this file.
- `amxmodx-libpawnc-litq-64bit.patch` is currently **NOT applied** (same
  `sc1.c` literal-queue hunk as `amxmodx-pawncc-64bit-literalpool.patch`,
  which IS applied). Verify overlap before deleting it.

## amxmodx core (`$SRC/amxmodx`)

- `amxmodx-pawncc-64bit.patch` — `Compile64` entry, `cellsize = sizeof(cell)`,
  drop `prefix.h`, `-DLINUX` for the host pawncc.
- `amxmodx-pawncc-64bit-literalpool.patch` — 64-bit literal pool in `sc1.c`
  (`newfunc` literal queue).
- `amxmodx-libpawnc-console.patch` — export `pc_printf`/`pc_error` with
  default visibility; make them work when `PAWN_CELL_SIZE=64`.
- `amxmodx-android-load-CModule.patch` — Android module loader: resolve
  modules from the app native-lib dir via `dladdr` (`CModule.cpp`).
- `amxmodx-android-load-modules.patch` — fall back to app native-lib dir
  when a module file is missing on disk (`modules.cpp`).
- `amxmodx-CDetour-cell.diff` — `cell_t32`/`cell_t64` selection via
  `#ifdef PAWN_CELL_SIZE` (CDetour).
- `amxmodx-64bit-cell-casts.diff` — cell casts in `file.cpp`.
- `amxmodx-memtools-dlfcn.diff` — `dlfcn.h` include for `MemoryUtils`
  (skip ELF fallback on Android: bionic `l_name` NULL → SIGSEGV).
- `amxmodx-CTextParsers-quote-underrun.diff` — quote-underrun guard in
  `CTextParsers`.
- `amxmodx-amtl-64bit.diff` — two-arg `Min/Max<T1,T2>` with `decltype`
  (AMTL, applied under `public/amtl`).
- `amxmodx-regparm-arm64.patch` — `regparm` attributes only on x86
  (`CvarManager.cpp`); ARM64 has no regparm ABI.
- `amxmodx-csx-string-guard.patch` — guard `STRING()`-of-fake-client netname
  wild pointers before `strlen()` (csx).
- `amxmodx-param-convert-64bit.patch` — resolve strings in the caller's
  address space with full 64-bit addresses (`natives.cpp`).
- `amxmodx-amx-hea-adopt.patch` — adopt heap growth from natives
  (`amx_Allot`); without it every `Allot` across SYSREQs reuses one block.
- `amxmodx-pcvar-handle-64bit.patch` — never truncate `cvar_t*` into a
  32-bit cell on ARM64; hand plugins handles instead.
- `amxmodx-float64-widen.patch` — float↔cell widening across core
  (`CvarManager`, `amxmodx.cpp`, `cvars`, `datapacks`, `string`).
- `amxmodx-gamesig-rtld.patch` — Android/Xash fallback: resolve game
  signatures from already-loaded handles, else walk `/proc/self/maps`
  (`CGameConfigs.cpp`).
- `amxmodx-interface-android.diff` — load the game library by its Android
  name (`libcs_android_arm64.so`) in `interface_helpers.h`.
- `amxmodx-ham-float64.patch` — `float` (not `REAL`/`double`) storage in
  hamsandwich `DataHandler`.
- `amxmodx-fakemeta-intvec-64.patch` — accept plain int arrays for
  vector/float pev fields (classic plugins pass `{255,255,255}` ints).
- `amxmodx-cbase-bit32-guard.diff` — guard ReGameDLL/ReHLDS arm64 small
  garbage values in `HLTypeConversion.h` (Android+aarch64 only).
- `amxmodx-ham-trampoline-arm64.patch` — C-only trampolines for aarch64
  (x86 template stays reachable on `__arm__`; `intptr_t` for `extraptr`).
- `amxmodx-cbase-pev-fallback.patch` — fallback for arm64 mods whose
  `CBaseEntity` layout relocates `pev` (`HLTypeConversion.h`).
- `amxmodx-fun-strip-user-weapons.diff` — strip weapons via engine
  directly; the fake-`player_weaponstrip`-entity round-trip breaks on
  current gamedlls.
- `amxmodx-module-suffix-arm.patch` — module file suffix: `_arm` on 32-bit
  ARM, `_amd64` on LP64/64-bit-cell builds.
- `amxmodx-pdata-runtime-translate.diff` — runtime pdata offset tables:
  arm64 ReGameDLL layout differs from legacy 32-bit linux (fakemeta +
  hamsandwich `pdata.cpp`).
- `amxmodx-sc6-state-dbginfo.patch` — skip empty automaton state names in
  `append_dbginfo` (state-machine plugins aborted amxxpc on
  `strlen(name)>0` assert); compiler logs under `ScriptFolder/logs`.

## metamod-p (`$SRC/metamod-p`, header source for the AMXX core)

- `metamod-p-aarch64.patch` — Makefile `arm64` targettype, ARM64
  `osdep_linkent`/`engineinfo`/`sdk_util`/`meta_api` fixes + new
  `metamod/cs16_amxx_compat.h` SDK type shim.
- `metamod-p-meta-debug-developer.patch` — do NOT auto-enable `meta_debug 3`
  in developer mode (it logs every engine call, hundreds of thousands of
  lines). Migrated from in-place edit `6bbecdd`.

## metamod-fwgs (`$SRC/metamod-fwgs`, the runtime gamemod)

- `metamod-fwgs-android.patch` — Android CWD fix (relative `cstrike/...`
  paths resolve from CWD) + CMake tweaks; builds
  `libmetamod_android_*.so`, renamed to `libmetamod.so`.

## vcs16 client tree (applied separately, see above)

- `mainui-menu-text-and-trim.patch` — CS16 menu text/trim customization
  (`Color.cpp`, `controls/Bitmap.cpp`, `menus/Main.cpp`).

## Reference only (never applied by the script)

- `regamedll-spawn-justconnected.diff` — first-spawn unarmed fix; live code
  comes from the fork branch (see workflow ReGameDLL step).

## Adding a new patch (recipe)

1. Reproduce the CI copy: copy the tree (`android/mm-p` submodule or
   fetched `$SRC/<name>`), `rm -rf .git`, `git init`, commit as `sourced`.
2. Edit, then `git diff > patches/<target>-<what>.patch`. Keep `---`/`+++`
   paths relative to the apply root (`$SRC/<name>` or the subdir).
3. Add one `apply_patch` line in `build-amxx.sh` in dependency order.
4. Verify: fresh copy → `git apply --check` → `git apply`, both must pass.
5. Add a one-liner to this README under the right target heading.
