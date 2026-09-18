package com.pickle.patcher.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.io.DEFAULT_BUFFER_SIZE

/**
 * Minimal GitHub Releases client. Fetches the latest release metadata and downloads
 * the AMXX mod bundle artifact so the patcher can inject freshly CI-built payloads
 * without shipping a compiler.
 */
object ReleaseRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Release(
        val tag_name: String = "",
        val name: String = "",
        val body: String = "",
        val published_at: String = "",
        val target_commitish: String = "",
        val assets: List<Asset> = emptyList(),
    ) {
        @Serializable
        data class Asset(
            val name: String = "",
            val browser_download_url: String = "",
            val size: Long = 0,
        )

        fun addonsAsset(): Asset? = assets.firstOrNull {
            it.name.startsWith("amxx-addons") && it.name.endsWith(".zip")
        }
    }

    @Serializable
    data class CompareResult(
        val commits: List<Commit> = emptyList(),
    ) {
        @Serializable
        data class Commit(
            val sha: String = "",
            val commit: CommitData = CommitData(),
        ) {
            @Serializable
            data class CommitData(
                val message: String = "",
            )
        }
    }

    suspend fun latest(repo: String): Release {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "cs16-amxx-patcher")
            .build()
        return client.newCall(req).execute().use { resp ->
            if (resp.code != 200) throw IOException("GitHub ${resp.code}: ${resp.message}")
            json.decodeFromString<Release>(resp.body?.string().orEmpty())
        }
    }

    /**
     * Fetches up to [perPage] most-recent releases and returns the newest one
     * that ships an `.apk` (a real APK update). The GitHub `releases/latest`
     * endpoint only points at the overall newest release, which may be a
     * bundle-only build with no APK asset — that bug made the app report
     * "up to date" while an older, APK-bearing release was still pending.
     */
    suspend fun latestApkRelease(repo: String, perPage: Int = 15): Release? {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases?per_page=$perPage")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "cs16-amxx-patcher")
            .build()
        return client.newCall(req).execute().use { resp ->
            if (resp.code != 200) throw IOException("GitHub ${resp.code}: ${resp.message}")
            val releases = json.decodeFromString<List<Release>>(resp.body?.string().orEmpty())
            releases.firstOrNull { rel -> rel.assets.any { it.name.endsWith(".apk", ignoreCase = true) } }
        }
    }

    /**
     * Fetches commits between two tags using the GitHub compare API.
     * Returns commit messages (first line of each) in reverse chronological order.
     */
    suspend fun compareCommits(repo: String, base: String, head: String): List<String> {
        return try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$repo/compare/$base...$head")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "cs16-amxx-patcher")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code != 200) return emptyList()
                val result = json.decodeFromString<CompareResult>(resp.body?.string().orEmpty())
                result.commits.map { it.commit.message.lineSequence().first().trim() }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun download(
        asset: Release.Asset,
        dest: File,
        onProgress: (Float) -> Unit = {},
    ): File = downloadUrl(asset.browser_download_url, dest, asset.size) { done, total ->
        if (total > 0) onProgress((done.toDouble() / total).toFloat().coerceIn(0f, 1f))
    }

    suspend fun downloadUrl(
        url: String,
        dest: File,
        knownSize: Long = 0,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): File {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "cs16-amxx-patcher")
            .header("Accept", "application/octet-stream")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download ${resp.code}")
            dest.parentFile?.mkdirs()
            val body = resp.body
                ?: throw IOException("Empty response body")
            val total = knownSize.takeIf { it > 0 }
                ?: body.contentLength().takeIf { it > 0 }
                ?: 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = 0L
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        onProgress(read, total)
                    }
                }
            }
        }
        return dest
    }
}