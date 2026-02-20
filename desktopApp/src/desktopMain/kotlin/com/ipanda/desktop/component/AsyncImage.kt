package com.ipanda.desktop.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.imageio.ImageIO

@Composable
fun AsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var isError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        isLoading = true
        isError = false
        try {
            val img = withContext(Dispatchers.IO) {
                ImageIO.read(URL(url))
            }
            bitmap = img?.toComposeImageBitmap()
            if (bitmap == null) isError = true
        } catch (e: Exception) {
            isError = true
        }
        isLoading = false
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            isError || bitmap == null -> {
                Text(
                    text = "🎬",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}
