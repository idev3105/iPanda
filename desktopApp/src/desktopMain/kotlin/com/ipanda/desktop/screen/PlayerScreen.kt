package com.ipanda.desktop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.ipanda.domain.StreamSource
import com.ipanda.domain.StreamType
import com.ipanda.domain.Constants
import java.util.Locale
import io.github.oshai.kotlinlogging.KotlinLogging
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import androidx.compose.ui.awt.SwingPanel
import java.awt.Component
import java.awt.Color as AwtColor

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerScreen(
    streamSources: List<StreamSource>,
    episodeTitle: String,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onOpenWebView: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedSourceIndex by remember { mutableStateOf(0) }
    var forceWebView by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val currentStreamSource = streamSources.getOrNull(selectedSourceIndex) ?: return

    // Auto-hide controls logic
    var showControls by remember { mutableStateOf(true) }
    var lastInteractedTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(showControls) {
        if (showControls) {
            while (true) {
                val timeSinceLast = System.currentTimeMillis() - lastInteractedTime
                val remaining = 5000 - timeSinceLast
                if (remaining <= 0) {
                    showControls = false
                    break
                }
                kotlinx.coroutines.delay(remaining)
            }
        }
    }

    fun interactionOccurred() {
        showControls = true
        lastInteractedTime = System.currentTimeMillis()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { interactionOccurred() })
            }
            .onPointerEvent(PointerEventType.Move) { interactionOccurred() }
    ) {
        // ── Video Player ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { interactionOccurred() },
                        onDoubleTap = { onToggleFullScreen() }
                    )
                }
                .onPointerEvent(PointerEventType.Move) { interactionOccurred() }
        ) {
            if (currentStreamSource.type == StreamType.IFRAME || forceWebView) {
                val webViewUrl = if (forceWebView) {
                    streamSources.find { it.type == StreamType.IFRAME }?.url ?: currentStreamSource.url
                } else {
                    currentStreamSource.url
                }
                
                LaunchedEffect(Unit) {
                    onOpenWebView(webViewUrl)
                }
            } else {
                VlcVideoPlayer(
                    url = currentStreamSource.url,
                    headers = currentStreamSource.headers,
                    onBuffering = { buffering -> isLoading = buffering },
                    onPlaying = { isLoading = false },
                    onError = { forceWebView = true },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Top bar ──
        AnimatedVisibility(
            visible = showControls || !isFullScreen,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = episodeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))

                    // Stream type badge
                    Surface(
                        color = when (currentStreamSource.type) {
                            StreamType.HLS -> Color(0xFF4CAF50)
                            StreamType.DASH -> Color(0xFF2196F3)
                            StreamType.MP4 -> Color(0xFFFF9800)
                            StreamType.IFRAME -> Color(0xFF9C27B0)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = currentStreamSource.type.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Controls bar ──
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Source selector
                            var showSourceMenu by remember { mutableStateOf(false) }
                            Box {
                                TextButton(
                                    onClick = { 
                                        interactionOccurred()
                                        showSourceMenu = true 
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Nguồn ${selectedSourceIndex + 1}")
                                }
                                DropdownMenu(
                                    expanded = showSourceMenu,
                                    onDismissRequest = { showSourceMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    streamSources.forEachIndexed { index, source ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    "Nguồn ${index + 1} (${source.type.name})",
                                                    fontWeight = if (index == selectedSourceIndex) FontWeight.Bold else FontWeight.Normal
                                                ) 
                                            },
                                            onClick = {
                                                selectedSourceIndex = index
                                                forceWebView = false
                                                showSourceMenu = false
                                                interactionOccurred()
                                            }
                                        )
                                    }
                                }
                            }

                            // Open in browser button
                            IconButton(
                                onClick = {
                                    interactionOccurred()
                                    onOpenWebView(currentStreamSource.url)
                                }
                            ) {
                                Text(
                                    text = "🌐",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Full screen button
                            IconButton(
                                onClick = {
                                    interactionOccurred()
                                    onToggleFullScreen()
                                }
                            ) {
                                Text(
                                    text = if (isFullScreen) "❐" else "⛶",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VlcVideoPlayer(
    url: String,
    headers: Map<String, String> = emptyMap(),
    onBuffering: (Boolean) -> Unit,
    onPlaying: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaPlayerComponent = remember {
        NativeDiscovery().discover()
        EmbeddedMediaPlayerComponent()
    }

    LaunchedEffect(url, headers) {
        // Wait for Native Window initialization bounds (helps prevent empty video frames on MacOS)
        kotlinx.coroutines.delay(500)

        // Optimized for HLS and generic streaming
        val options = mutableListOf(
            "--network-caching=3000",
            "--hls-live-edge=3",
            "--clock-jitter=0",
            "--clock-synchro=0",
            "--avcodec-hw=none" // Fixes hardware acceleration black screen issues on macOS
        )
        
        headers.forEach { (key, value) ->
            if (key.equals("referer", ignoreCase = true)) {
                options.add(":http-referrer=$value")
            } else if (key.equals("user-agent", ignoreCase = true)) {
                options.add(":http-user-agent=$value")
            }
        }
        
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun error(mediaPlayer: MediaPlayer?) {
                logger.error { "VLC Player error occurred for URL: $url" }
                onError()
            }

            override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
                onBuffering(newCache < 100f)
            }

            override fun playing(mediaPlayer: MediaPlayer?) {
                onPlaying()
            }
        })
        
        mediaPlayerComponent.mediaPlayer().media().play(url, *options.toTypedArray())
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerComponent.mediaPlayer().release()
        }
    }

    SwingPanel(
        factory = {
            mediaPlayerComponent.apply {
                background = AwtColor.BLACK
            }
        },
        modifier = modifier
    )
}
