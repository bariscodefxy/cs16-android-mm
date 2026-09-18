# Progress — Nexora

## What works
- Full native cross-compile: `libamxmodx.so` + 13 modules (cstrike csx engine
  fakemeta fun geoip hamsandwich json nvault reapi regex sockets sqlite) +
  `libmetamod.so` + `libyapb.so` + `libclient_android_*.so` + host/device
  pawncc + 64-bit `.amxx` plugins (`ALL_BUILT`).
- ReGameDLL `libcs_android_arm64.so` with first-spawn fix, bundled for arm64.
- Patcher: ABI validation, exact-lib prune, STORED+aligned inject, V1+V2
  sign + verify, structured `PatchReport`.
- Incremental update via `manifest.json` (SHA-256 per `.so`, ABI-prefixed
  assets); `amxx-addons.zip` packaging; single-job release with Discord notify.
- App: Patch/Compile/Addons screens, on-device `.sma` compile with log files,
  addons auto-install + status scan, crash-log view + share, self-update flow.
- hlsdk/mm-p submodule conversion compiles clean in CI (`0ee1ac3` green:
  full-header mm-p + both metamod patches → `ALL_BUILT` on arm64).
- `patcherlib` first unit tests green locally (Temurin 17):
  `BundleManifestTest` 3/3 + `BundleTest` 2/2 + `ZipRawTest` 4/4
  (`:patcherlib:test` 9 tests 0 failure); dead-code cleanup + nav wiring
  (`releases`/`crashlog` routes) included, uncommitted.

## What's left
- On-device runtime smoke test (pending).
- `patcherlib` test coverage is starter-level (manifest/zip/bundle only).
- `armeabi-v7a` on-device validation per module.
- `patches/` upkeep against upstream `master` drift (amxmodx + metamod-p).
- One `[android build]` CI run for the Gradle APK side
  (`:app:assembleRelease`) — bundle jobs skip it.

## Known issues (migrated from CHANGES.md)
- On-device runtime smoke test pending; 32-bit `.amxx` intentionally rejected.
- v7a gaps: hamsandwich trampolines C-only for aarch64, arm32 pdata layout
  untested, arm64-only `libcs` byte-patch doesn't apply.
- Historical (fixed, don't regress): `lib/<abi>/lib` prefix prune deleted the
  engine; hardcoded `cellsize=4` rejected all plugins; `MIN`/`MAX` template
  mixing; `CDetour` x86 trampoline crash on ARM64; amxxpc `lib` prefix resolve;
  bionic `dlmap->l_name` SIGSEGV; ARM FZ flushing denormals; `Trampolines.h`
  pointer truncation.

## Decision evolution
`CHANGES.md` → retired and deleted; history lives here + `systemPatterns.md`.
`metamod-fwgs` fork approach → abandoned in favor of `metamod-p` headers +
`metamod-fwgs` runtime. Full-bundle zip → incremental manifest + addons zip.
3-job CI → single job (artifact quota). `vcs16` copy → submodule.
Vendored `android/hlsdk` + `android/mm-p` → pinned submodules
(alliedmodders/hlsdk@a0edb77, Bots-United/metamod-p@7ec9b01); in-place
`metamod.cpp` tweak → `patches/metamod-p-meta-debug-developer.patch`;
mm-p header shims dropped (full upstream tree).
