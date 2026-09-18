# Sync issues — dead-code cleanup

Durum temizdir ve açık çakışma yoktur. ReleasesScreen ve CrashLogScreen NavHost ve overflow menüye bağlandı, artık ölü ekran kalmadı. patcherlib tarafı gerçek derleyici ve unit test ile doğrulandı (Temurin JDK 17, :patcherlib:compileKotlin + :patcherlib:jar + :patcherlib:test BUILD SUCCESSFUL, 9 test 0 failure, GRADLE_EXIT=0). CI `build & release` 3c1e544 üzerinde completed success verdi (APK bacağı dahil). Görev kullanıcı onayıyla kapatıldı. Bilinçli olarak korunmuş kapsam: BundleBuilder ve legacy gen-bundle.py (test ve legacy altyapı) ile tema paleti.
