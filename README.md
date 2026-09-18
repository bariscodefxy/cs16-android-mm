# Nexora

**Nexora** patches the CS 1.6 client APK on your Android device to bundle
**AMX Mod X** and your plugins, so AMXX runs without ever touching the source
app twice. Pick the APK, hit **Patch**, install the output — done.

## Fork differences (vs upstream [berkchy/nexora](https://github.com/berkchy/nexora))

This repo is a fork of `berkchy/nexora` — the patcher concept, 64-bit-cell
AMXX port, crash-handler client and ReGameDLL first-spawn fix all come from
there. What this fork does differently:

- **Canonical SDK submodules** — `android/hlsdk` (`alliedmodders/hlsdk`) and
  `android/mm-p` (`Bots-United/metamod-p@master`) instead of upstream's
  `berkchy/hlsdk` + `berkchy/mm-p` forks under `3rdparty/`. Provenance
  verified by SHA-256: hlsdk is the Valve 2.3p3 mirror (279/279 files
  byte-identical), not FWGS portable. The `meta_debug 3` auto-enable fix
  stays a patch file (`patches/metamod-p-meta-debug-developer.patch`);
  upstream carries it inside its mm-p fork instead.
- **`patches/` as the only diff carrier** — `patches/README.md` catalogs
  every native customization with an add-a-patch recipe; neither submodules
  nor CI-fetched trees are ever edited in place.
- **Full addons tree kept** — all game plugins stay in the repo
  (`cstrike/dod/esf/ns/tfc/ts` `.amxx`, `zombie_plague40.amxx`,
  `configs/cstrike/`, test `hello`); upstream stripped everything non-CS
  and made its extractor never overwrite existing configs.
- **`android/plugins-src` kept** — the `example.sma` smoke-test plugin is
  still compiled to 64-bit `.amxx` in CI; upstream dropped the directory.
- **SHA-256 incremental updates** — per-`.so` SHA-256 `manifest.json`;
  upstream moved to size-based checks.
- **App hygiene** — Downloads (Releases) and Crash log screens wired into
  the `NavHost` with back-arrow navigation, dead-code cleanup, and the first
  `patcherlib` unit tests (`BundleManifestTest`, `BundleTest`, `ZipRawTest`
  — 9 tests green).
- **CI model** — single job on every branch push refreshing the rolling
  `continuous` prerelease; versioned releases only from commits containing
  "release" + `vX.Y.Z` (or manual dispatch); `[android build]` /
  `[bundle build]` flags gate only the APK leg. Upstream releases from tags
  with its own versioning (1.27.x).
- **Docs as memory-bank** — `memory-bank/` + `AGENTS.md` / `CLAUDE.md`
  instead of upstream's `CHANGES.md`.
- **Sync note** — fork point predates upstream v1.27.11, so newer upstream
  work is not yet pulled in: bold DHUD text, `ClientDisconnect` forwards
  (metamod + AMXX `xash-disconnect-forwards`), the `strip_user_weapons`
  SIGSEGV fix, and the flat lib-list UI.

## How it works

1. **Patch** — Select the CS 1.6 client `.apk` on your device. A component
   checklist pops up (everything checked; uncheck to skip). Nexora injects the
   AMXX core, Metamod and the plugin set, re-signs it, and produces a ready
   to-install APK.
2. **Install** — Install the generated APK. The app identity and signature are kept,
   so your account and game data survive.
3. **Compile** — Drop `.sma` sources into `addons/amxmodx/scripting/`. Nexora compiles
   them to 64-bit `.amxx` and includes them at the next Patch.
4. **Addons** — The full AMXX package is embedded in the app, so it works offline.

## Install

- Download the latest `nexora_vX.Y.Z.apk` from the **Releases** page
  (every push also refreshes the rolling `continuous` prerelease).
- Allow "install from unknown sources" and install the APK.
- Open the app and pick your CS 1.6 APK from the **Patch** tab.

## Requirements

- **arm64-v8a** device (supported; `armeabi-v7a` is best-effort trial).
- Android 7.0+ (API 24).
- A CS 1.6 client APK (Xash3D-based).

## Build (for developers)

Clone with submodules (`git clone --recursive`), then:

```sh
# Native: fetch upstream + apply patches/ + NDK cross-compile (needs NDK r25c)
bash android/ci/build-amxx.sh "$PWD/src" "$NDK_ROOT" "$PWD/out" android/plugins-src arm64-v8a
# -> out/lib/<abi>/*.so, out/compiler/<abi>/amxxpc*, out/plugins/*.amxx (ends with ALL_BUILT)

# Incremental manifest
RELEASE_VERSION=<tag> python3 android/ci/gen-manifest.py arm64-v8a out/lib/arm64-v8a out-manifest --plugins-dir out/plugins

# APK (from android/)
./gradlew :patcherlib:test
./gradlew :app:assembleRelease
```

Toolchain: JDK 17, NDK `r25c` (`25.2.9519653`), SDK `platforms;android-36` +
`build-tools;35.0.0`, CMake + Ninja. See `AGENTS.md` and `memory-bank/` for
the full workflow (commit-message flags `[android build]` / `[bundle build]`
control the CI APK leg).
