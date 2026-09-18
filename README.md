# Nexora

**Nexora** patches the CS 1.6 client APK on your Android device to bundle
**AMX Mod X** and your plugins, so AMXX runs without ever touching the source
app twice. Pick the APK, hit **Patch**, install the output — done.

## Fork features (what this adds over the stock client)

Upstream sources are fetched fresh in CI and customized only via `patches/`
(see `patches/README.md`). The forks/pins used:

- `vcs16/` — [`berkchy/vcs16`](https://github.com/berkchy/vcs16) submodule
  (CS16 Xash3D client fork), built to `libclient_android_*.so`:
  - **Crash handler** — native crash backtraces written to `crash.log`
    (fresh `INIT` header on every init; crash-safe frame-pointer walk via
    `process_vm_readv`; guarded game-dir resolve; re-armed in `HUD_VidInit`;
    hex maps parsing with exec-bit check, `.dynsym` fallback and
    fault-address attribution). Viewable from the app's **Crash log** screen.
  - **MainUI menu customization** (`mainui-menu-text-and-trim.patch`) —
    trimmed single-player leftovers (Hazard Course, Save/Restore, Previews),
    banner draws the last button text directly, missing `Color.cpp`
    placeholder for upstream build break.
- **ReGameDLL first-spawn fix** — [`berkchy/ReGameDLL_CS@fix/first-spawn-equip`](https://github.com/berkchy/ReGameDLL_CS)
  branch, shipped as `libcs_android_arm64.so` (arm64). A round-restart respawn
  firing right after team-join left the player alive but unarmed (no weapon,
  no ammo HUD); the fix promotes the in-team spawn to a regular in-game spawn.
  If the component is unchecked, the patcher falls back to byte-patching the
  base APK's `libcs` instead.
- **64-bit-cell AMX Mod X port** (`PAWN_CELL_SIZE=64`) — a 32-bit cell cannot
  hold a function pointer on arm64, so the core, all 13 modules
  (`cstrike csx engine fakemeta fun geoip hamsandwich json nvault reapi regex
  sockets sqlite`), the on-device Pawn compiler and every plugin are built for
  64-bit cells. 32-bit `.amxx` files are rejected by design.
- **Metamod** — headers from `Bots-United/metamod-p`, runtime from
  `FWGS/metamod-fwgs` (injected as `libyapb_android_*.so` for `-dll @yapb`);
  `meta_debug 3` no longer auto-enables in developer mode. `plugins.ini`
  chains AMXX + [YaPB](https://github.com/yapb/yapb) bots; [ReAPI](https://github.com/rehlds/ReAPI)
  is included. `libmenu` is intentionally not shipped (stock menu stays).
- **Patcher app extras** — per-component checklist before patching (uncheck
  anything to skip it), per-`.so` SHA-256 incremental updates via
  `manifest.json`, offline-first embedded bundle, on-device `.sma` → `.amxx`
  compilation (logs under `addons/amxmodx/scripting/logs/`), crash-log viewer,
  and app self-update polling.

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
