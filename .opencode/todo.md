# Dead-code cleanup — TODO

status: completed
scope: android/app + android/patcherlib ölü kod temizliği (yalnızca silme, davranış değişikliği yok)

## Leaves (all verified)
- [x] Ölü import/duplicate temizliği (History/Delete/FilterChipDefaults/Gray70/Gray85/CrashLogScreen/heightIn/LinearProgressIndicator/rememberScrollState/withContext/JsonElement/FileInputStream/FileOutputStream/Color + MainActivity remember ailesi)
- [x] Ölü fonksiyon/alan temizliği (ZipAnalyzer.entryState, ZipRaw.requiresAlignment, CrashLog.readLatest, ReleaseRepository.bundleAsset+latestTagRedirect/webClient, SigningKeystore.fingerprintSha1, Verification.sameSigner, PatcherViewModel.useLoadedBundle+CACHE_TAG)
- [x] Ölü local/parametre temizliği (arscBefore, progressLast, modSuffix x2 + getTargetPath param, total/inp/buf)
- [x] Referans taraması — silinen sembollere kalan çağrı yok (grep kanıtlı)
- [x] LSP diagnostics (*) clean + git diff --check clean
- [x] Brace/paren denge taraması temiz; Signer.kt + ZipRaw.kt ayraç onarımı okuma ile doğrulandı
- [x] Graph re-index + check_index_coverage 15/15 no_recorded_issue; search_graph yapısal kanıt (Verification 170-178, ZipEntryInfo 43-52 kapalı; verify ApkSignerTool 180-194; silinenler 0 hit)
- [x] Memory bank güncellemesi (activeContext current focus)
- [x] Ölü ekranların bağlanması — ReleasesScreen + CrashLogScreen NavHost (releases/crashlog) + overflow menü (Downloads/Crash log) + geri oku; banktaki tanımlı UX geri geldi
- [x] Gerçek derleyici kanıtı — Temurin JDK 17 kuruldu, :patcherlib:compileKotlin + :patcherlib:jar BAŞARILI (GRADLE_EXIT=0); :patcherlib:test NO-SOURCE (bankta kayıtlı no-op, test yok)
- [x] patcherlib unit testleri (AGENTS.md gereği) — BundleManifestTest 3/3, BundleTest 2/2, ZipRawTest 4/4; toplam 9 test 0 failure; :patcherlib:test BAŞARILI

## Verification evidence (prose)
LSP diagnostics file wildcard clean olarak wiring sonrası tekrar koşuldu. git diff check clean sonucudur ve git diff stat 16 dosya 37 insertion 105 deletion göstermektedir (wiring öncesi 15 insertion 104 deletion idi). Brace paren denge taraması MainActivity wiring sonrası tekrar koşuldu ve dengelidir. Graph reindex fast modda 31110 node ile tamamlandı ve coverage kontrolü 15 dosyada kayıtlı sorun bulamadı. Gradle patcherlib test ve app assembleRelease kutuda önceden koşamıyordu; Temurin JDK 17 kurulduktan sonra :patcherlib:compileKotlin ve :patcherlib:jar BUILD SUCCESSFUL ile geçti (GRADLE_EXIT=0, 2m5s). Ardından AGENTS.md gereği ilk unit testler eklendi (BundleManifestTest, ZipRawTest, BundleTest) ve :patcherlib:test 9 test 0 failure ile BAŞARILI oldu (GRADLE_EXIT=0). :app derlemesi Android SDK gerektirir ve yalnızca CI android build ile kapanır.
