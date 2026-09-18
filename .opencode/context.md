# Context — dead-code cleanup

- objective: Kullanılmayan dead codeları kaldır
- bank: MEMORY_BANK.md + projectbrief/productContext/activeContext/systemPatterns/techContext/progress
- scope fit: yalnızca doğrulanmış ölü kod silindi; davranış değişikliği yok, feature silinmedi
- verification: LSP `*` clean; git diff --check clean; brace/paren taraması temiz (2 onarılan ayraç dahil); grep referans taraması temiz; graph re-index yapısal kanıt; :patcherlib gerçek-derleme + 9 unit test BAŞARILI (GRADLE_EXIT=0); :app yalnızca CI
- residual risk: düşük — silinenlerin tamamı grep ile çağrısız olduğu kanıtlı; yine de `[android build]` CI green beklenmeli
- follow-on: görev kullanıcı onayıyla kapatıldı (3c1e544 pushed + CI success + `continuous`→3c1e544)
