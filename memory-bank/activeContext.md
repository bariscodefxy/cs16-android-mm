# Active Context — Nexora

## Current focus
Submodule conversion VALIDATED on the native side: CI green on `0ee1ac3`
(`[bundle build]`) — full-header `mm-p` tree + `aarch64`/`meta-debug`
patches compile to `ALL_BUILT`. Gradle APK side (`:app:assembleRelease`)
did NOT run in that job (bundle-only flag) — still needs one
`[android build]` CI run for full green.

## Recent changes (from git log)
- amxxpc state-machine assertion fix (`amxmodx-sc6-state-dbginfo.patch`) +
  compiler logs under `ScriptFolder/logs` (`compiler.log`/`error.log`).
- `vcs16` vendored as submodule (`berkchy/vcs16`); crash-handler work
  (crash-safe FP walk via `process_vm_readv`, crash.log recreate on init,
  `resolveGameDir` outsize fix, re-arm in `HUD_VidInit`).
- ReGameDLL `libcs` (first-spawn fix) bundled as arm64 component.
- Patch component-selection popup (uncheckable bundle entries).
- Release pipeline: single-job workflow, APK-vs-bundle Discord notifications
  with thumbnail/image + download link, `meta_debug 3` auto-enable stopped.
- hlsdk/mm-p vendored → submodules (pins above). Provenance proof: hlsdk is
  alliedmodders/hlsdk, NOT FWGS (old FWGS gitlink was the removed mm-fwgs
  experiment); mm-p ≈ upstream HEAD with 4 local diffs (3 header shims
  dropped, meta_debug → patch, 2 deleted files restored).
- Dead `amxx-addons` branch checkout removed from CI (`0ee1ac3`);
  `addons/` ships on master, workflow verifies presence.
- `patches/README.md` catalog (per-patch docs + add-a-patch recipe) +
  `build-amxx.sh` section map; normalized 2 stale `diff --git` headers
  (`amxmodx-android-load-*.patch` pointed at author-local paths).

## Next steps
1. Commit + push the staged hlsdk/mm-p submodule conversion; first CI run
   must show native `ALL_BUILT` (full-header mm-p) + Gradle green — neither
   runs on the Windows dev box.
2. On-device runtime smoke test (still pending — native + patcher verified in
   CI/local only).
3. Add `patcherlib` unit tests (`src/test/` empty despite JUnit dep).
4. Validate `armeabi-v7a` per module on-device before offering as release.
5. Keep `patches/` in sync with `amxmodx@master` + `metamod-p@master` drift
   (both rolling).

## Active decisions / patterns
- All native customization via `patches/` + `build-amxx.sh`; never edit
  submodules or fetched trees in place.
- `addons/` edited directly on master (the `amxx-addons` branch is gone;
  its CI checkout step became a presence check).
- Exact-path prune only; `libmenu` never ships; 32-bit `.amxx` rejected by design.
