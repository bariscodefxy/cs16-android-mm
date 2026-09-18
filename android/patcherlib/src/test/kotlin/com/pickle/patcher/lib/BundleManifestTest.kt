package com.pickle.patcher.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the bundle manifest schema and the exact-path prune rule:
 * pruning must never contain a bare `lib/<abi>/` prefix (that once
 * deleted the whole engine) — only exact lib paths plus `META-INF/`.
 */
class BundleManifestTest {

    @Test
    fun parseEmptyJsonYieldsDefaults() {
        val m = BundleManifest.parse("{}")
        assertEquals("", m.version)
        assertEquals("cs16client", m.game)
        assertEquals("arm64-v8a", m.abi)
        assertTrue(m.entries.isEmpty())
    }

    @Test
    fun encodeParseRoundTripPreservesEntries() {
        val manifest = BundleManifest(
            version = "v0.0.1",
            game = "cs16client",
            abi = "arm64-v8a",
            entries = listOf(
                BundleManifest.BundleEntry(
                    source = "lib/arm64-v8a/libamxmodx.so",
                    target = "lib/arm64-v8a/libamxmodx.so",
                    method = BundleManifest.Compression.STORED,
                    required = true,
                    description = "AMX Mod X core",
                ),
                BundleManifest.BundleEntry(
                    source = "lib/arm64-v8a/libcstrike_amxx_amd64.so",
                    target = "lib/arm64-v8a/libcstrike_amxx_amd64.so",
                    method = BundleManifest.Compression.DEFLATED,
                    required = false,
                    description = "cstrike module",
                ),
            ),
        )
        val parsed = BundleManifest.parse(BundleManifest.encode(manifest))
        assertEquals(manifest, parsed)
    }

    @Test
    fun defaultExcludeRulePrunesOnlyExactLibsAndMetaInf() {
        val prefixes = ExcludeRule.DEFAULT.prefixes
        assertTrue(prefixes.contains("META-INF/"))
        assertTrue(prefixes.contains("lib/arm64-v8a/libamxmodx.so"))
        assertTrue(prefixes.contains("lib/arm64-v8a/libmetamod.so"))
        // No bare ABI directory prefix: that pattern deleted the engine once.
        assertTrue(prefixes.none { it == "lib/arm64-v8a/" || it == "lib/armeabi-v7a/" || it == "lib/" })
        assertTrue(ExcludeRule.DEFAULT.exact.isEmpty())
    }
}
