package com.ipanda.desktop.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipanda.desktop.component.AsyncImage
import com.ipanda.domain.Episode
import com.ipanda.domain.EpisodeGroup
import com.ipanda.domain.Movie
import com.ipanda.domain.StreamSource
import com.ipanda.domain.repository.MovieRepository
import com.ipanda.domain.repository.StreamRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}


@Composable
fun MovieDetailScreen(
    movieUrl: String,
    movieRepository: MovieRepository,
    streamRepository: StreamRepository,
    favoriteRepository: com.ipanda.domain.repository.FavoriteRepository,
    onBack: () -> Unit,
    onPlayStream: (List<StreamSource>, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var movie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Currently selected episode group + loaded episodes
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    // Sniffing state
    var sniffingEpisode by remember { mutableStateOf<String?>(null) }
    var sniffError by remember { mutableStateOf<String?>(null) }

    // Load movie detail
    LaunchedEffect(movieUrl) {
        try {
            movie = withContext(Dispatchers.IO) { movieRepository.getMovieDetail(movieUrl) }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    LaunchedEffect(movie?.id) {
        movie?.id?.let { id ->
            favoriteRepository.isFavorite(id).collect { isFavorite = it }
        }
    }

    // Separate effect for episodes
    LaunchedEffect(movie) {
        movie?.episodeGroups?.firstOrNull()?.let { group ->
            if (group.url.isNotEmpty()) {
                isLoadingEpisodes = true
                episodes = withContext(Dispatchers.IO) { movieRepository.getEpisodes(group.url) }
                isLoadingEpisodes = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ──
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Quay lại", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = movie?.title ?: "Chi tiết phim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lỗi: $error", color = MaterialTheme.colorScheme.error)
                }
            }
            movie == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy phim", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                val m = movie!!
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    // ── Movie Info Row ──
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Poster
                        AsyncImage(
                            url = m.posterUrl,
                            contentDescription = m.title,
                            modifier = Modifier
                                .width(220.dp)
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )

                        Spacer(Modifier.width(24.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = m.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            if (isFavorite) {
                                                favoriteRepository.removeFavorite(m)
                                            } else {
                                                favoriteRepository.addFavorite(m)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(if (isFavorite) "❤️ Đã thích" else "🤍 Thêm vào yêu thích")
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            if (m.year > 0) {
                                InfoChip(label = "Năm", value = "${m.year}")
                                Spacer(Modifier.height(4.dp))
                            }

                            if (m.viewCount.isNotEmpty()) {
                                InfoChip(label = "Lượt xem", value = m.viewCount)
                                Spacer(Modifier.height(4.dp))
                            }

                            Spacer(Modifier.height(12.dp))

                            if (m.plot.isNotEmpty()) {
                                Text(
                                    text = "Nội dung",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = m.plot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (m.description.isNotEmpty() && m.description != m.plot) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = m.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    // ── Episode Groups Tabs ──
                    if (m.episodeGroups.isNotEmpty()) {
                        Text(
                            text = "Danh sách tập phim",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(12.dp))

                        ScrollableTabRow(
                            selectedTabIndex = selectedGroupIndex,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            m.episodeGroups.forEachIndexed { index, group ->
                                Tab(
                                    selected = selectedGroupIndex == index,
                                    onClick = {
                                        selectedGroupIndex = index
                                        scope.launch {
                                            isLoadingEpisodes = true
                                            try {
                                                episodes = if (group.url.isNotEmpty()) {
                                                    withContext(Dispatchers.IO) { movieRepository.getEpisodes(group.url) }
                                                } else {
                                                    group.episodes
                                                }
                                            } catch (e: Exception) {
                                                episodes = emptyList()
                                            }
                                            isLoadingEpisodes = false
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = group.title,
                                            fontWeight = if (selectedGroupIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Episodes ──
                        when {
                            isLoadingEpisodes -> {
                                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                            }
                            episodes.isEmpty() -> {
                                Text("Không có tập phim", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            else -> {
                                EpisodeGrid(
                                    episodes = episodes,
                                    sniffingEpisode = sniffingEpisode,
                                    onEpisodeClick = { episode ->
                                        sniffError = null
                                        sniffingEpisode = episode.url
                                        scope.launch {
                                            try {
                                                 val streams = withContext(Dispatchers.IO) { streamRepository.getStreamUrl(episode.url) }
                                                  if (streams.isNotEmpty()) {
                                                      onPlayStream(streams, episode.title)
                                                  } else {
                                                      logger.warn { "Failed to sniff stream url for episode: ${episode.url}" }
                                                    sniffError = "Không tìm thấy link phát cho: ${episode.title}"
                                                }
                                            } catch (e: Exception) {
                                                sniffError = "Lỗi: ${e.message}"
                                            }
                                            sniffingEpisode = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Sniff error
                    sniffError?.let {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeGrid(
    episodes: List<Episode>,
    sniffingEpisode: String?,
    onEpisodeClick: (Episode) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        episodes.forEach { episode ->
            val isSniffing = sniffingEpisode == episode.url
            OutlinedButton(
                onClick = { if (!isSniffing) onEpisodeClick(episode) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSniffing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isSniffing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isSniffing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
