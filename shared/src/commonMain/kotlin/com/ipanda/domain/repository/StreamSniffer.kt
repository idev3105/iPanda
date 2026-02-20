package com.ipanda.domain.repository

import com.ipanda.domain.StreamSource

interface StreamSniffer {
    suspend fun sniffStreamViaIframe(episodeUrl: String): List<StreamSource>
}
