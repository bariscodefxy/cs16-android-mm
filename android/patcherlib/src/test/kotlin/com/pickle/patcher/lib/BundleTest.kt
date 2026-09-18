package com.pickle.patcher.lib

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Bundle parsing plus the component-selection filter used before patching. */
class BundleTest {

    @Test
    fun fromZipParsesManifestAndPayloadFiles() {
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val manifest = BundleManifest(
            version = "live",
            game = "cs16client",
            abi = "arm64-v8a",
            entries = listOf(
                BundleManifest.BundleEntry(
                    source = "lib/arm64-v8a/libamxmodx.so",
                    target = "lib/arm64-v8a/libamxmodx.so",
                ),
            ),
        )
        val zipBytes = buildBundleZip(manifest, mapOf("lib/arm64-v8a/libamxmodx.so" to payload))

        val bundle = Bundle.fromZip(zipBytes)
        assertEquals("live", bundle.manifest.version)
        assertEquals(1, bundle.manifest.entries.size)
        assertContentEquals(payload, bundle.resolveEntry(bundle.manifest.entries.single()))
    }

    @Test
    fun withEntriesFiltersManifestButKeepsBlobs() {
        val manifest = BundleManifest(
            version = "live",
            entries = listOf(
                BundleManifest.BundleEntry(source = "a.so", target = "lib/arm64-v8a/a.so"),
                BundleManifest.BundleEntry(source = "b.so", target = "lib/arm64-v8a/b.so"),
            ),
        )
        val bundle = Bundle(
            manifest,
            mapOf("a.so" to byteArrayOf(1), "b.so" to byteArrayOf(2)),
        )
        val filtered = bundle.withEntries(bundle.manifest.entries.take(1))
        assertEquals(1, filtered.manifest.entries.size)
        assertEquals("a.so", filtered.manifest.entries.single().source)
        // Blobs stay complete so re-selection never needs a re-download.
        assertEquals(2, filtered.files.size)
        assertNull(filtered.resolveEntry(BundleManifest.BundleEntry(source = "missing.so", target = "x")))
    }

    private fun buildBundleZip(manifest: BundleManifest, files: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("bundle.json"))
            zos.write(BundleManifest.encode(manifest).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            for ((name, content) in files) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
