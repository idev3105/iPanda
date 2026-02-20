package com.ipanda.desktop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.ipanda.domain.Constants
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    val webViewState = rememberWebViewState(url)
    val navigator = rememberWebViewNavigator()
    val blockRegex = remember { Regex(Constants.BLOCK_REGEX, RegexOption.IGNORE_CASE) }

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

    LaunchedEffect(webViewState.lastLoadedUrl) {
        webViewState.lastLoadedUrl?.let { currentUrl ->
            if (blockRegex.containsMatchIn(currentUrl)) {
                navigator.stopLoading()
                logger.info { "Blocked request/navigation by regex: $currentUrl" }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { interactionOccurred() })
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // WebView
            WebView(
                state = webViewState,
                navigator = navigator,
                modifier = Modifier.fillMaxSize()
            )

            // Top bar
            AnimatedVisibility(
                visible = showControls,
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
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
