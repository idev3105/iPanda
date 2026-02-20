package com.ipanda.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ipanda.desktop.screen.PlayerScreen
import com.ipanda.domain.StreamSource
import com.ipanda.domain.StreamType

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Test Player Independent") {
        PlayerScreen(
            streamSources = listOf(
                StreamSource(
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    type = StreamType.HLS
                ),
                StreamSource(
                    url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    type = StreamType.MP4
                )
            ),
            episodeTitle = "Test Episode: Big Buck Bunny",
            isFullScreen = false,
            onToggleFullScreen = { println("Toggle Full Screen") },
            onBack = { println("Back pressed") }
        )
    }
}
