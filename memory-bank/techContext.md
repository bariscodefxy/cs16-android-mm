# Tech Context — Nexora

## Technologies
- App: Kotlin 2.3.21, Jetpack Compose (Material3, navigation-compose),
  coroutines, kotlinx-serialization-json, OkHttp, AGP 8.13.2.
- Patch engine: pure-JVM Kotlin (`patcherlib`), `apksig:8.13.2` (V1+V2 sign),
  JUnit4 declared (no `src/test/` yet).
- Native: C/C++14/17, ARM64 + ARM32 trampolines (`natives-android.c`
  generated per ABI), static libc++ (`libc++_static` whole-archive,
  `assert_shim.c` wraps `__assert2`/`__assert_fail`), static pcre 8.45,
  CMake+Ninja (metamod-fwgs, yapb, vcs16, regamedll), Pawn `libpc300`
  (`Compile64`, `PAWN_CELL_SIZE=64`).

## Development setup
- JDK 17 (Temurin), NDK `r25c` (`25.2.9519653`), SDK `platforms;android-36` +
  `build-tools;35.0.0`, CMake + Ninja. Gradle wrapper in `android/` only.
- `git clone --recursive` (vcs16 + hlsdk + mm-p submodules). Missing
  `vcs16/3rdparty` pins are fetched by `build-amxx.sh` — don't hand-copy.
- Native: `bash android/ci/build-amxx.sh "$PWD/src" "$NDK_ROOT" "$PWD/out"
  "" arm64-v8a` → `ALL_BUILT` (`plugins-src` dir dropped; 4th arg stays `""`).
- APK: `cd android && ./gradlew :app:assembleRelease`
  (`APP_VERSION_NAME=vX.Y.Z`, else `0.0.1`). Signing: `android/debug/patcher-release.p12`.
- Manifest: `RELEASE_VERSION=<tag> python3 android/ci/gen-manifest.py arm64-v8a
  out/lib/arm64-v8a out-manifest --plugins-dir out/plugins`.

## Constraints
- `compileSdk/targetSdk 36`, `minSdk 24` (app; `ApkPatcher` default 21),
  `ANDROID_PLATFORM android-24` (android-21 for regamedll), NDK r25c pinned.
- `PAWN_CELL_SIZE=64` mandatory for core + modules + compiler + plugins.
- Payload ABI matrix: arm64-v8a `*_amxx_amd64.so` (supported),
  armeabi-v7a `*_amxx_arm.so` (trial; hamsandwich trampolines, arm32 pdata,
  `libcs` byte-patch untested there).
- `.so` ZIP entries: STORED, 16 KB-aligned; `resources.arsc`: STORED, 4-aligned.
- Never commit `*.keystore`, `build/`, `*.tgz`, `*.log`, fetched trees
  (`src/`, `build-out/`, `rgdll-*`). `[android build]`/`[version build]` in head
  commit message controls APK build in CI. Every branch push refreshes the
  rolling `continuous` prerelease; a versioned release needs "release" +
  `vX.Y.Z` in the message (e.g. `[release] v0.0.2`) or manual dispatch
  `inputs.version`.
