package com.ipanda.data.repository

import com.ipanda.domain.StreamSource
import com.ipanda.domain.repository.StreamRepository
import com.ipanda.domain.repository.StreamSniffer
import com.ipanda.util.currentTimeMillis
import com.ipanda.util.readCache
import com.ipanda.util.writeCache
import com.ipanda.util.clearAllCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PersistentCacheEntry(
    val sources: List<StreamSource>,
    val timestampMs: Long
)

class StreamRepositoryImpl(
    private val sniffer: StreamSniffer
) : StreamRepository {

    private val mutex = Mutex()
    private val cacheDurationMs = 24 * 60 * 60 * 1000L // 1 day
    private val json = Json { ignoreUnknownKeys = true }

    private fun getCacheKey(url: String): String {
        // Use a safe filename from the URL hash
        return "stream_${url.hashCode()}.json"
    }

    override suspend fun getStreamUrl(episodeUrl: String): List<StreamSource> {
        val cacheKey = getCacheKey(episodeUrl)

        // Try to load from persistent cache
        val cachedContent = try {
            readCache(cacheKey)
        } catch (e: Exception) {
            null
        }

        if (cachedContent != null) {
            try {
                val entry = json.decodeFromString<PersistentCacheEntry>(cachedContent)
                if (currentTimeMillis() - entry.timestampMs < cacheDurationMs) {
                    return entry.sources
                }
            } catch (e: Exception) {
                // Ignore corrupted cache
            }
        }

        // Do expensive sniffing
        var result = sniffer.sniffStreamViaIframe(episodeUrl)

        if (result.isEmpty()) {
            result = listOf(
                StreamSource(
                    url = episodeUrl,
                    type = com.ipanda.domain.StreamType.IFRAME,
                    quality = "auto"
                )
            )
        }

        if (result.isNotEmpty()) {
            mutex.withLock {
                try {
                    val entry = PersistentCacheEntry(result, currentTimeMillis())
                    writeCache(cacheKey, json.encodeToString(entry))
                } catch (e: Exception) {
                    // Ignore write errors
                }
            }
        }
        return result
    }

    override suspend fun clearCache() {
        mutex.withLock {
            clearAllCache()
        }
    }
}

