# Nexora — CS 1.6 (Xash3D) AMX Mod X patcher for Android

Nexora (`applicationId com.nexora`, code namespace `com.pickle.patcher`) is an
on-device APK patcher: pick a stock CS 1.6 client APK (Xash3D-based), it injects
a 64-bit-cell AMX Mod X core + Metamod + modules + addons, re-signs it, and the
output installs over the original (identity/signature kept). It also ships an
on-device Pawn compiler (`.sma` → `.amxx`).

Upstream sources are NEVER vendored as code — they are fetched fresh in CI and
customized only via `patches/`. Project memory lives in `MEMORY_BANK.md`
(protocol) + `memory-bank/` (projectbrief, productContext, systemPatterns,
techContext, activeContext, progress): read ALL bank files at the start of
every task, and update the bank after significant changes.

## Repo layout — what is what

- `android/app/` — Compose UI + orchestration (the only Android module).
  `MainActivity.kt` (bottom-nav: Patch / Compile / Addons + overflow: Plugins,
  Releases, CrashLog), `patcher/PatcherViewModel.kt` (all patch/compile/addons/
  update logic), `data/IncrementalUpdateManager.kt` (per-`.so` SHA-256 update
  from `manifest.json`), `data/BundleProvider.kt`, `data/ReleaseRepository.kt`.
- `android/patcherlib/` — pure-JVM patch engine, no Android deps:
  `ApkPatcher.kt` (analyze → inject → align → sign → verify),
  `ZipRepacker.kt` (ABI prune, exact-lib exclude, 16 KB-aligned STORED `.so`,
  `libcs` byte-patch), `ZipRaw.kt` / `ZipAnalyzer.kt` (low-level ZIP),
  `BundleManifest.kt` (`bundle.json` + `ExcludeRule.DEFAULT`),
  `BundleBuilder.kt`, `Signer.kt` (`ApkSignerTool` over `apksig`, V1+V2),
  `Cli.kt`. Package `com.pickle.patcher.lib`.
- `android/ci/` — `build-amxx.sh` (THE native build: fetch upstream + apply
  `patches/` + NDK cross-compile core/modules/metamod/pawncc/plugins),
  `gen-manifest.py` (current: `manifest.json` + per-ABI staged `.so` files for
  incremental update), `gen-bundle.py` (legacy full-bundle zip),
  `assert_shim.c` (links `__assert2`/`__assert_fail` against static libc++).
- `android/hlsdk/` + `android/mm-p/` — vendored Half-Life SDK headers and
  Bots-United/metamod-p (header source for the AMXX core). Do NOT edit in
  place; change `patches/` instead.
- `android/plugins-src/` — drop `.sma` here (+ optional `include/`); CI
  compiles them to 64-bit `.amxx`. `example.sma` is the smoke-test plugin.
- `android/debug/` — `patcher-release.p12` + pem files. CI copies these into
  `app/src/main/assets/keystore/`. Never commit real keys (`*.keystore` is
  gitignored).
- `patches/` (~32 files) — every native customization:
  `amxmodx-*` (64-bit cells `PAWN_CELL_SIZE=64`, Android dlopen loader, ARM64
  trampoline/CDetour guards, module suffix `_arm` vs `_amd64`, pawncc
  `Compile64`), `metamod-p-aarch64.patch` + `metamod-fwgs-android.patch`,
  `regamedll-spawn-justconnected.diff` (first-spawn unarmed fix → `libcs`),
  `mainui-menu-text-and-trim.patch`.
- `addons/` — runtime data tree, checked out in CI from the `amxx-addons`
  branch (NOT master): `amxmodx/` (configs, plugins, `scripting/`), `metamod/`
  (`config.ini`, `plugins.ini` → `linux addons/amxmodx/libamxmodx.so`).
  Editing this tree never needs an APK rebuild.
- `vcs16/` — git submodule `berkchy/vcs16@main` (CS16 Xash3D client fork).
  Built in CI to `libclient_android_*.so` (crash handler). Clone recursive.
- `.github/workflows/build-and-release.yml` — single-job pipeline (tag `v*` or
  manual dispatch): NDK cross-compile → ReGameDLL `libcs` → manifest/addons
  packaging → optional APK → GitHub Release → Discord notify.
- Native payload (arm64-v8a primary, `*_amxx_amd64.so`; armeabi-v7a trial,
  `*_amxx_arm.so`): `libamxmodx.so`, `libmetamod.so` (injected as
  `libyapb_android_*.so`), 13 modules (`cstrike csx engine fakemeta fun geoip
  hamsandwich json nvault reapi regex sockets sqlite`), `libyapb.so`,
  `libclient_android_*.so`, `libcs_android_*.so`. `libmenu` is intentionally
  NOT shipped (broken text menu — stock menu stays).

## Dev environment tips

- Clone with submodules: `git clone --recursive <url>` (vcs16 + amxmodx amtl).
  If `vcs16/3rdparty/*` is empty, `build-amxx.sh` fetches `mainui_cpp` /
  `miniutl` pins itself — don't hand-copy them.
- Toolchain: JDK 17 (Temurin in CI), Android NDK `r25c` (`25.2.9519653`),
  `platforms;android-36` + `build-tools;35.0.0`, CMake + Ninja. Gradle wrapper
  lives in `android/` — run all Gradle from there, there is no root wrapper.
- Native build (Linux runner, NDK required):
  `bash android/ci/build-amxx.sh "$PWD/src" "$NDK_ROOT" "$PWD/out" android/plugins-src arm64-v8a`
  Args: `<src-root> <ndk-root> <out-dir> [plugins-src] [abi]`. Output:
  `out/lib/<abi>/*.so`, `out/compiler/<abi>/amxxpc{,32.so}`, `out/plugins/*.amxx`.
  Expect `ALL_BUILT` at the end; `armeabi-v7a` failure is non-fatal by design.
- APK build: `cd android && ./gradlew :app:assembleRelease`. CI sets
  `APP_VERSION_NAME=vX.Y.Z` (falls back to `0.1.0` locally) and embeds
  `build-out/compiler/<abi>/amxxpc*` as `jniLibs/<abi>/libamxxpc{,32}.so`.
  Signing uses `android/debug/patcher-release.p12` (`android`/`androiddebugkey`).
- `addons/` on master is a placeholder — CI runs
  `git checkout origin/amxx-addons -- addons/` before packaging. Edit addons on
  the `amxx-addons` branch, not here.
- Patch engine rule (don't regress): `ExcludeRule.DEFAULT` prunes ONLY the
  exact 13 AMXX/metamod lib paths + `META-INF/` — never a `lib/<abi>/lib`
  prefix match (that once deleted the whole engine). `.so` entries must be
  STORED + aligned (`resources.arsc` STORED + 4-byte aligned); `ApkPatcher`
  throws on any misaligned STORED entry.
- Commit-message flags drive CI: `[android build]` / `[version build]` in the
  head commit builds the APK; bundle/libs always build. Keep these tags when
  native/patcher changes need a user-visible APK.

## Testing instructions

- There are no committed unit tests (`patcherlib` declares JUnit but has no
  `src/test/`). Do not claim `gradle test` covers anything — it is a no-op
  until tests are added. If you change `patcherlib`, add/extend tests under
  `android/patcherlib/src/test/` and run them.
- From `android/`: `./gradlew :patcherlib:test` (unit), `./gradlew :app:assembleRelease`
  (APK must sign + `apksig` verify clean). Commit only when both pass.
- Native check: `bash android/ci/build-amxx.sh …` must print `ALL_BUILT` and
  list `libamxmodx.so` + all 13 modules + `libmetamod.so` under
  `out/lib/arm64-v8a/`. Then
  `RELEASE_VERSION=<tag> python3 android/ci/gen-manifest.py arm64-v8a out/lib/arm64-v8a out-manifest --plugins-dir out/plugins`
  must emit `manifest.json` with matching SHA-256/sizes.
- Patcher self-checks to preserve: output has zero misaligned STORED entries,
  `resources.arsc` STORED + 4-aligned, V1+V2 signature verifies, same-signer
  fingerprint kept. On-device runtime smoke test is still pending — say so in
  PRs instead of claiming it.
- 32-bit `.amxx` is intentionally rejected by the 64-bit core; `example.sma`
  must compile to a loadable 64-bit plugin after any compiler change.

## PR instructions

- Title format: `[android build] <Title>` for APK/patcher/native changes,
  `[bundle build] <Title>` for libs/plugins-only changes (mirrors
  `git log --oneline`). `ci:` / `debug:` prefixes are allowed for workflow-only
  tweaks. Releases ship from `v*` tags; body = head commit message.
- Always run `./gradlew :patcherlib:test` and `./gradlew :app:assembleRelease`
  (from `android/`) before committing; for native changes also run
  `build-amxx.sh` for `arm64-v8a` to `ALL_BUILT`.
- Never commit `*.keystore`, `build/`, `*.tgz`, `*.log` (gitignored); never
  commit fetched upstream trees (`src/`, `build-out/`, `rgdll-*`) or real
  signing keys. Keep `patches/` as the only native diff carrier.
