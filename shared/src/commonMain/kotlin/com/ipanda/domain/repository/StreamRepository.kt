package com.ipanda.domain.repository

import com.ipanda.domain.StreamSource

interface StreamRepository {
    suspend fun getStreamUrl(episodeUrl: String): List<StreamSource>
    suspend fun clearCache()
}
