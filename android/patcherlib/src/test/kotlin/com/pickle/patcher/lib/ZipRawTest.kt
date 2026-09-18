package com.pickle.patcher.lib

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Structural checks for the byte-preserving ZIP reader. */
class ZipRawTest {

    @Test
    fun openMissingFileReturnsNull() {
        assertNull(ZipRaw.open(java.io.File("/nonexistent/path/source.apk")))
    }

    @Test
    fun openShortBytesReturnsNull() {
        assertNull(ZipRaw.open(ByteArray(10)))
    }

    @Test
    fun openGarbageBytesReturnsNull() {
        assertNull(ZipRaw.open("this is definitely not a zip archive".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun stdlibZipRoundTripReadsStoredAndDeflated() {
        val storedPayload = "stored-payload".toByteArray(Charsets.UTF_8)
        val deflatedPayload = "deflated-payload-".repeat(200).toByteArray(Charsets.UTF_8)
        val bytes = buildZip(
            stored = "assets/data.bin" to storedPayload,
            deflated = "lib/arm64-v8a/libx.so" to deflatedPayload,
        )

        val zip = ZipRaw.open(bytes) ?: error("std zip should parse")
        try {
            assertEquals(2, zip.entries.size)
            val stored = zip.entries.getValue("assets/data.bin")
            assertEquals(0, stored.method)
            assertContentEquals(storedPayload, zip.readContent(stored))
            val deflated = zip.entries.getValue("lib/arm64-v8a/libx.so")
            assertEquals(8, deflated.method)
            assertContentEquals(deflatedPayload, zip.readContent(deflated))
            // Raw deflated bytes must survive the round trip untouched.
            assertTrue(zip.readRaw(deflated).isNotEmpty())
        } finally {
            zip.close()
        }
    }

    private fun buildZip(
        stored: Pair<String, ByteArray>,
        deflated: Pair<String, ByteArray>,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            val crc = CRC32()
            crc.update(stored.second)
            val se = ZipEntry(stored.first)
            se.method = ZipEntry.STORED
            se.size = stored.second.size.toLong()
            se.compressedSize = stored.second.size.toLong()
            se.crc = crc.value
            zos.putNextEntry(se)
            zos.write(stored.second)
            zos.closeEntry()
            val de = ZipEntry(deflated.first)
            de.method = ZipEntry.DEFLATED
            zos.putNextEntry(de)
            zos.write(deflated.second)
            zos.closeEntry()
        }
        return out.toByteArray()
    }
}
