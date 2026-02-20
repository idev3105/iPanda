package com.ipanda.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.ipanda.domain.StreamSource
import com.ipanda.domain.StreamType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val streamsJson = intent.getStringExtra(EXTRA_STREAMS) ?: "[]"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val streamSources = Json.decodeFromString<List<StreamSource>>(streamsJson)

        // Enable immersive full-screen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setContent {
            com.ipanda.android.ui.iPandaTheme {
                PlayerContent(
                    streamSources = streamSources,
                    episodeTitle = title,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        private const val EXTRA_STREAMS = "extra_streams"
        private const val EXTRA_TITLE = "extra_title"

        fun launch(context: Context, streams: List<StreamSource>, title: String) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAMS, Json.encodeToString(streams))
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerContent(
    streamSources: List<StreamSource>,
    episodeTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedSourceIndex by remember { mutableStateOf(0) }
    var forceWebView by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Sync with ExoPlayer controller visibility
    var showControls by remember { mutableStateOf(true) }

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
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                logger.error(error) { "Player error occurred: ${error.message}" }
                                forceWebView = true
                                isLoading = false
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                            }
                        })
                    }
            }

            LaunchedEffect(streamSource) {
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

            if (streamSource.type == StreamType.IFRAME || forceWebView) {
                LaunchedEffect(Unit) {
                    val fallbackUrl = if (forceWebView) {
                        streamSources.find { it.type == StreamType.IFRAME }?.url ?: streamSource.url
                    } else {
                        streamSource.url
                    }
                    WebViewActivity.launch(context, fallbackUrl)
                    onBack()
                }
            } else {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            setShowFastForwardButton(false)
                            setShowRewindButton(false)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                                showControls = visibility == View.VISIBLE
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = com.ipanda.android.ui.NetflixRed)
                    }
                }

                // Top Bar Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
                            .statusBarsPadding()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
