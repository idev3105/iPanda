package com.ipanda.crawler

import com.ipanda.domain.StreamSource
import com.ipanda.domain.StreamType

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StreamSnifferTest {

    @Test
    fun `sniffStream returns valid stream from real site`() = runBlocking {
        // Ensure you have browserless running on port 3000
        // docker run -p 3000:3000 ghcr.io/browserless/chromium
        
        val sniffer = StreamSniffer(
            browserlessApiKey = System.getenv("BROWSERLESS_API_KEY") ?: "",
            browserlessEndpoint = System.getenv("BROWSERLESS_ENDPOINT") ?: ""
        )

        // buying time for the crawler to actually find something. 
        // We use a known movie page.
        // target: https://hhhtq.team/xem/250-sv1-ep1/
        val targetUrl = "https://www.dailymotion.com/embed/video/x8cs0v2"
        
        println("Sniffing stream from $targetUrl ...")
        val streamSources = sniffer.sniffStream(targetUrl)
        
        println("Found stream: $streamSources")

        assertNotNull(streamSources, "Stream source should not be null")
        assertTrue(streamSources.isNotEmpty(), "Stream URL should not be empty")
    }
    
    @Test
    fun `extractIframe returns valid iframe url`() = runBlocking {
        val sniffer = StreamSniffer(
            browserlessApiKey = "",
            browserlessEndpoint = "http://127.0.0.1:3000/function/"
        )

        val targetUrl = "https://hhhtq.team/xem/164-sv1-ep188/"
        println("Extracting iframe from $targetUrl ...")

        val iframeUrl = sniffer.extractIframe(targetUrl)
        println("Found iframe: $iframeUrl")

        assertNotNull(iframeUrl, "Iframe URL should not be null")
        assertTrue(iframeUrl.isNotEmpty(), "Iframe URL should not be empty")
        assertTrue(
            iframeUrl.contains("odysee.com") || iframeUrl.startsWith("http"),
            "Iframe URL should be valid (contain odysee.com or start with http), got: $iframeUrl"
        )
    }
    @Test
    fun `sniffStreamViaIframe returns valid stream`() = runBlocking {
        val sniffer = StreamSniffer(
            browserlessApiKey = "",
            browserlessEndpoint = "http://127.0.0.1:3000/function/"
        )
        // This URL is expected to have an iframe that contains the stream
        val targetUrl = "https://hhhtq.team/xem/250-sv1-ep8/"
        println("Sniffing stream via iframe from $targetUrl ...")

        val streamSources = sniffer.sniffStreamViaIframe(targetUrl)
        println("Found via iframe stream: $streamSources")

        assertNotNull(streamSources, "Stream source via iframe should not be null")
        assertTrue(streamSources.isNotEmpty(), "Stream URL should not be empty")
    }
}
