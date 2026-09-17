# Active Context — Nexora

## Current focus
Memory-bank migration: `CHANGES.md` (old maintainer memory) is retired into
`memory-bank/`; `CLAUDE.md` now points at `MEMORY_BANK.md` + this bank.
Single source of truth going forward is the bank, not `CHANGES.md`.

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

## Next steps
1. On-device runtime smoke test (still pending — native + patcher verified in
   CI/local only).
2. Add `patcherlib` unit tests (`src/test/` empty despite JUnit dep).
3. Validate `armeabi-v7a` per module on-device before offering as release.
4. Keep `patches/` in sync with `amxmodx@master` drift (master is rolling 1.10).

## Active decisions / patterns
- All native customization via `patches/` + `build-amxx.sh`; never edit
  vendored `hlsdk`/`mm-p` or fetched trees in place.
- `addons/` edited on `amxx-addons` branch, not master.
- Exact-path prune only; `libmenu` never ships; 32-bit `.amxx` rejected by design.
