package com.ipanda.android.screen

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.ipanda.android.WebViewActivity
import com.ipanda.domain.StreamSource
import com.ipanda.domain.StreamType
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamSources: List<StreamSource>,
    episodeTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedSourceIndex by remember { mutableStateOf(0) }
    var forceWebView by remember { mutableStateOf(false) }
    val streamSource = streamSources.getOrNull(selectedSourceIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (streamSource == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No stream available", color = Color.White)
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        } else {
            val exoPlayer = remember(streamSource) {
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                streamSource.headers.filterKeys { key ->
                    !key.startsWith(":") &&
                    !key.equals("range", ignoreCase = true) &&
                    !key.equals("if-range", ignoreCase = true) &&
                    !key.equals("accept-encoding", ignoreCase = true)
                }.let { headers ->
                    httpDataSourceFactory.setDefaultRequestProperties(headers)
                }

                val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                    .apply {
                        playWhenReady = true
                    }
            }

            LaunchedEffect(streamSource) {
                logger.info { "Setting up player with stream url: ${streamSource.url}" }
                logger.info { "Setting up player with headers: ${streamSource.headers}" }
                if (streamSource.type != StreamType.IFRAME) {
                    val mediaItem = MediaItem.fromUri(streamSource.url)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                }
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    exoPlayer.release()
                }
            }

            if (streamSource.type == StreamType.IFRAME) {
                // Launch dedicated WebViewActivity for IFRAME - avoids Compose SurfaceView conflict
                LaunchedEffect(Unit) {
                    WebViewActivity.launch(context, streamSource.url)
                    onBack()
                }
            } else {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            // Netflix red for progress bar (requires custom theme or view surgery, basic surgery here)
                            setShowFastForwardButton(false)
                            setShowRewindButton(false)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Cinematic Overlays
                Box(Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = episodeTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                exoPlayer.pause()
                                WebViewActivity.launch(context, streamSource.url)
                            }
                        ) {
                            Icon(Icons.Filled.Public, contentDescription = "Open in WebView", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
