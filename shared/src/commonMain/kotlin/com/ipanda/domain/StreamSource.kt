package com.ipanda.domain

import kotlinx.serialization.Serializable

@Serializable
data class StreamSource(
    val url: String,
    val type: StreamType,
    val quality: String = "auto",
    val headers: Map<String, String> = emptyMap()
)

@Serializable
enum class StreamType {
    HLS, // .m3u8
    DASH, // .mpd
    MP4,
    IFRAME
}
