package com.pickle.patcher.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages incremental updates using GitHub Releases API.
 * Compares file sizes between release assets and local libs/ directory.
 * No manifest.json needed — just size comparison.
 */
object IncrementalUpdateManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class ReleaseAsset(
        val name: String = "",
        val size: Long = 0,
        val browser_download_url: String = "",
    )

    @Serializable
    data class ReleaseInfo(
        val tag_name: String = "",
        val assets: List<ReleaseAsset> = emptyList(),
    )

    data class AssetInfo(
        val assetName: String,
        val cleanName: String,
        val size: Long,
        val downloadUrl: String,
    )

    data class UpdateResult(
        val toDownload: List<AssetInfo>,
        val upToDate: List<AssetInfo>,
        val totalBytes: Long,
    )

    private const val REPO = "bariscodefxy/cs16-android-mm"

    /**
     * Fetch release assets from GitHub API and filter for current ABI.
     * Returns AssetInfo with clean names (stripped ABI prefix).
     */
    suspend fun fetchReleaseAssets(tag: String, abi: String): List<AssetInfo> {
        val url = "https://api.github.com/repos/$REPO/releases/tags/$tag"
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "cs16-amxx-patcher")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val release = json.decodeFromString<ReleaseInfo>(body)
                release.assets
                    .filter { it.name.startsWith("${abi}__") && it.name.endsWith(".so") }
                    .map { asset ->
                        val cleanName = asset.name.removePrefix("${abi}__")
                        AssetInfo(
                            assetName = asset.name,
                            cleanName = cleanName,
                            size = asset.size,
                            downloadUrl = asset.browser_download_url,
                        )
                    }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Compare release assets against local files on disk.
     * Uses file size as the comparison metric.
     */
    fun diff(releaseAssets: List<AssetInfo>, libsDir: File, abi: String): UpdateResult {
        val targetDir = File(libsDir, abi)
        val toDownload = mutableListOf<AssetInfo>()
        val upToDate = mutableListOf<AssetInfo>()

        for (asset in releaseAssets) {
            val fileOnDisk = File(targetDir, asset.cleanName)
            if (fileOnDisk.exists() && fileOnDisk.length() == asset.size) {
                upToDate.add(asset)
            } else {
                toDownload.add(asset)
            }
        }

        val totalBytes = toDownload.sumOf { it.size }
        return UpdateResult(toDownload = toDownload, upToDate = upToDate, totalBytes = totalBytes)
    }

    /**
     * Download only files that need updating.
     * Saves with clean name (no ABI prefix) — files are in ABI subdirectory.
     */
    suspend fun downloadChanged(
        toDownload: List<AssetInfo>,
        libsDir: File,
        abi: String,
        onFileStart: (index: Int, asset: AssetInfo) -> Unit = { _, _ -> },
        onFileProgress: (index: Int, asset: AssetInfo, progress: Float) -> Unit = { _, _, _ -> },
        onProgress: (downloaded: Int, total: Int, bytesWritten: Long) -> Unit = { _, _, _ -> },
    ) {
        val targetDir = File(libsDir, abi)
        targetDir.mkdirs()

        for ((index, asset) in toDownload.withIndex()) {
            onFileStart(index, asset)

            val destFile = File(targetDir, asset.cleanName)
            destFile.parentFile?.mkdirs()

            val req = Request.Builder()
                .url(asset.downloadUrl)
                .header("User-Agent", "cs16-amxx-patcher")
                .header("Accept", "application/octet-stream")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Download failed for ${asset.assetName}: ${resp.code}")
                }
                val body = resp.body ?: throw IllegalStateException("Empty body for ${asset.assetName}")
                val expectedSize = asset.size.takeIf { it > 0 }
                    ?: body.contentLength().takeIf { it > 0 }
                    ?: 0L
                var written = 0L
                body.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            written += n
                            if (expectedSize > 0) {
                                onFileProgress(index, asset, (written.toFloat() / expectedSize).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                destFile.setExecutable(true)
            }

            onProgress(index + 1, toDownload.size, toDownload.take(index + 1).sumOf { it.size })
        }
    }

    /**
     * Download a single file from the release.
     */
    suspend fun downloadSingle(
        asset: AssetInfo,
        libsDir: File,
        abi: String,
        onProgress: (Float) -> Unit = {},
    ) {
        val targetDir = File(libsDir, abi)
        targetDir.mkdirs()

        val destFile = File(targetDir, asset.cleanName)
        destFile.parentFile?.mkdirs()

        val req = Request.Builder()
            .url(asset.downloadUrl)
            .header("User-Agent", "cs16-amxx-patcher")
            .header("Accept", "application/octet-stream")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Download failed for ${asset.assetName}: ${resp.code}")
            }
            val body = resp.body ?: throw IllegalStateException("Empty body for ${asset.assetName}")
            val expectedSize = asset.size.takeIf { it > 0 }
                ?: body.contentLength().takeIf { it > 0 }
                ?: 0L
            var written = 0L
            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        written += n
                        if (expectedSize > 0) {
                            onProgress((written.toFloat() / expectedSize).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            destFile.setExecutable(true)
        }
    }

    /**
     * Load all .so files from libs/<abi>/ for patching.
     * Returns Map<targetPath, fileBytes> compatible with ZipRepacker.
     */
    fun loadBundleFiles(libsDir: File, abi: String): Map<String, ByteArray> {
        val targetDir = File(libsDir, abi)
        if (!targetDir.exists()) return emptyMap()

        val suffix = if (abi == "arm64-v8a") "arm64" else "armv7l"

        val files = HashMap<String, ByteArray>()
        targetDir.listFiles()?.filter {
            it.isFile && it.extension == "so" && !it.name.startsWith("libmenu_")
        }?.forEach { file ->
            val name = file.name
            val targetPath = getTargetPath(name, abi, suffix)
            if (targetPath != null) {
                files[targetPath] = file.readBytes()
            }
        }
        return files
    }

    /**
     * Map a clean .so filename to its target path in the APK.
     */
    private fun getTargetPath(name: String, abi: String, suffix: String): String? {
        return when {
            name == "libmetamod.so" -> "lib/$abi/libyapb_android_$suffix.so"
            name.startsWith("lib") && name.endsWith(".so") -> "lib/$abi/$name"
            name.endsWith(".so") -> "lib/$abi/lib$name"
            else -> null
        }
    }
}
