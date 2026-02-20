package com.ipanda.crawler

import com.ipanda.domain.StreamSource
import com.ipanda.domain.StreamType
import com.ipanda.domain.repository.StreamSniffer as IStreamSniffer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class StreamSniffer(
    var browserlessApiKey: String,
    var browserlessEndpoint: String
) : IStreamSniffer {
    private val scriptTemplate: String by lazy {
        this::class.java.getResource("/scrapers/stream-sniffer.js")?.readText()
            ?: throw IllegalStateException("Script /scrapers/stream-sniffer.js not found in resources")
    }

    private val iframeExtractorScriptTemplate: String by lazy {
        this::class.java.getResource("/scrapers/iframe-extractor.js")?.readText()
            ?: throw IllegalStateException("Script /scrapers/iframe-extractor.js not found in resources")
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun sniffStream(targetUrl: String): List<StreamSource> {
        // JavaScript snippet to run in Browserless
        // This script intercepts requests to find .m3u8 or .mpd
        val functionBody = scriptTemplate.replace("__TARGET_URL__", targetUrl)

        try {
            // Manually serialize to ensure correct JSON format
            val jsonBody = Json.encodeToString(BrowserlessRequest(code = functionBody))
            logger.debug { "Sending request to Browserless at $browserlessEndpoint" }
            
            val response = client.post(browserlessEndpoint) {
                header(HttpHeaders.Accept, "*/*")
                if (browserlessApiKey.isNotBlank()) {
                    parameter("token", browserlessApiKey)
                }
                setBody(TextContent(jsonBody, ContentType.Application.Json))
            }

            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.body<String>()
                
                // Parse the JSON array of stream objects
                val sniffedStreams = try {
                    Json.decodeFromString<List<SniffedStream>>(responseBody)
                } catch (e: Exception) {
                    // Fallback if the response is a list of strings or a single string
                    try {
                        Json.decodeFromString<List<String>>(responseBody).map { SniffedStream(it) }
                    } catch (e2: Exception) {
                        val trimmed = responseBody.trim()
                        if (trimmed.isNotBlank() && !trimmed.startsWith("[")) {
                            listOf(SniffedStream(trimmed))
                        } else {
                            emptyList()
                        }
                    }
                }

                return sniffedStreams.filter { it.url.isNotBlank() }.map { item ->
                    val url = item.url
                    val type = when {
                        url.contains(".m3u8") -> StreamType.HLS
                        url.contains(".mpd") -> StreamType.DASH
                        else -> StreamType.MP4
                    }

                    // Ensure Referer is present as it's often required for playback
                    val headers = item.headers.toMutableMap()
                    headers["referer"] = targetUrl

                    logger.info { "Found stream URL: $url ($type)" }
                    logger.debug { "Stream headers: $headers" }
                    StreamSource(url = url.trim(), type = type, headers = headers)
                }
            } else {
                val errorBody = response.body<String>()
                logger.error { "Browserless error: ${response.status}" }
                logger.error { "Response body: $errorBody" }
                return emptyList()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error sniffing stream from $targetUrl" }
            return emptyList()
        }
    }

    suspend fun extractIframe(targetUrl: String): String? {
        val functionBody = iframeExtractorScriptTemplate.replace("__TARGET_URL__", targetUrl)
        logger.debug { "Prepared script for $targetUrl" }

        try {
            logger.debug { "Sending iframe extraction request to Browserless at $browserlessEndpoint" }
            
            // Send the script directly as application/javascript content type
            val response = client.post(browserlessEndpoint) {
                header(HttpHeaders.Accept, "*/*")
                if (browserlessApiKey.isNotBlank()) {
                    parameter("token", browserlessApiKey)
                }
                setBody(TextContent(functionBody, ContentType.parse("application/javascript")))
            }

            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.body<String>()
                // Browserless returns the string result directly
                return responseBody.takeIf { it.isNotBlank() }?.trim().also {
                    logger.info { "Found iframe URL: $it" }
                }
            } else {
                val errorBody = response.body<String>()
                logger.error { "Browserless error (iframe extract): ${response.status}" }
                logger.error { "Response body: $errorBody" }
                return null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting iframe from $targetUrl" }
            return null
        }
    }

    override suspend fun sniffStreamViaIframe(targetUrl: String): List<StreamSource> {
        val iframeUrl = extractIframe(targetUrl)
        return if (iframeUrl != null) {
            logger.info { "Extracted iframe URL: $iframeUrl. Proceeding to sniff stream from iframe." }
            sniffStream(iframeUrl)
        } else {
            logger.warn { "No iframe found on $targetUrl" }
            emptyList()
        }
    }
}

@Serializable
data class BrowserlessRequest(
    val code: String
)

@Serializable
data class SniffedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)
