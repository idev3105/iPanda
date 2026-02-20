package com.ipanda.android.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipanda.android.ui.BackgroundDark
import com.ipanda.android.ui.NetflixRed
import com.ipanda.android.ui.OnSurfaceMuted
import com.ipanda.android.ui.SurfaceDark
import com.ipanda.android.ui.SurfaceVariant
import com.ipanda.android.ui.shimmerEffect
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    movieUrl: String,
    movieRepository: MovieRepository,
    streamRepository: StreamRepository,
    favoriteRepository: com.ipanda.domain.repository.FavoriteRepository,
    onBack: () -> Unit,
    onPlayClick: (List<StreamSource>, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var movie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    var sniffingEpisodeUrl by remember { mutableStateOf<String?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(movieUrl) {
        try {
            movie = withContext(Dispatchers.IO) { movieRepository.getMovieDetail(movieUrl) }
        } catch (e: Exception) {
            errorDialogMessage = "Lỗi load chi tiết phim: ${e.localizedMessage ?: "Không xác định"}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(movie?.id) {
        movie?.id?.let { id ->
            favoriteRepository.isFavorite(id).collect { isFav ->
                isFavorite = isFav
            }
        }
    }

    // Separate effect for episodes to avoid re-checking favorite status on group change
    LaunchedEffect(movie) {
        movie?.episodeGroups?.firstOrNull()?.let { group ->
            isLoadingEpisodes = true
            episodes = if (group.url.isNotEmpty()) {
                withContext(Dispatchers.IO) { movieRepository.getEpisodes(group.url) }
            } else {
                group.episodes
            }
            isLoadingEpisodes = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NetflixRed)
            }
        } else {
            movie?.let { m ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Cinematic Backdrop
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        AsyncImage(
                            model = m.posterUrl,
                            contentDescription = m.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, BackgroundDark)
                                    )
                                )
                        )
                    }
                    
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = m.title,
                            style = MaterialTheme.typography.h4,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Action Buttons
                        DetailActions(
                            isSniffing = sniffingEpisodeUrl == m.url,
                            isFavorite = isFavorite,
                            onPlayClick = {
                                if (sniffingEpisodeUrl == null) {
                                    sniffingEpisodeUrl = m.url
                                    scope.launch {
                                        try {
                                            val streams = withContext(Dispatchers.IO) { streamRepository.getStreamUrl(m.url) }
                                            if (streams.isNotEmpty()) {
                                                onPlayClick(streams, m.title)
                                            } else {
                                                errorDialogMessage = "Không load được stream"
                                            }
                                        } catch (e: Exception) {
                                            errorDialogMessage = "Lỗi: ${e.localizedMessage}"
                                        } finally {
                                            sniffingEpisodeUrl = null
                                        }
                                    }
                                }
                            },
                            onFavoriteClick = {
                                scope.launch {
                                    if (isFavorite) {
                                        favoriteRepository.removeFavorite(m)
                                    } else {
                                        favoriteRepository.addFavorite(m)
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var isExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = m.description,
                            style = MaterialTheme.typography.body2,
                            color = OnSurfaceMuted,
                            lineHeight = 20.sp,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isExpanded) "Ẩn bớt" else "Xem thêm",
                            color = Color.White,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { isExpanded = !isExpanded }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (m.episodeGroups.isNotEmpty()) {
                            EpisodeSection(
                                groups = m.episodeGroups,
                                selectedGroupIndex = selectedGroupIndex,
                                episodes = episodes,
                                isLoadingEpisodes = isLoadingEpisodes,
                                sniffingUrl = sniffingEpisodeUrl,
                                onGroupSelected = { index, group ->
                                    selectedGroupIndex = index
                                    scope.launch {
                                        isLoadingEpisodes = true
                                        episodes = try {
                                            if (group.url.isNotEmpty()) {
                                                withContext(Dispatchers.IO) { movieRepository.getEpisodes(group.url) }
                                            } else group.episodes
                                        } catch (e: Exception) { emptyList() }
                                        isLoadingEpisodes = false
                                    }
                                },
                                onEpisodeClick = { episode ->
                                    if (sniffingEpisodeUrl == null) {
                                        sniffingEpisodeUrl = episode.url
                                        scope.launch {
                                            try {
                                                val streams = withContext(Dispatchers.IO) { streamRepository.getStreamUrl(episode.url) }
                                                if (streams.isNotEmpty()) {
                                                    onPlayClick(streams, episode.title)
                                                } else {
                                                    errorDialogMessage = "Không load được stream"
                                                }
                                            } catch (e: Exception) {
                                                errorDialogMessage = "Lỗi: ${e.localizedMessage}"
                                            } finally {
                                                sniffingEpisodeUrl = null
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Back Button Overlay
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            backgroundColor = SurfaceDark,
            contentColor = Color.White,
            title = { Text("Thông báo") },
            text = { Text(errorDialogMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { errorDialogMessage = null }) {
                    Text("OK", color = NetflixRed)
                }
            }
        )
    }
}

@Composable
fun DetailActions(
    isSniffing: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onPlayClick,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            shape = RoundedCornerShape(4.dp)
        ) {
            if (isSniffing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Phát", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onFavoriteClick)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) NetflixRed else Color.White
            )
            Text(
                text = "Yêu thích",
                style = MaterialTheme.typography.caption,
                color = if (isFavorite) NetflixRed else Color.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EpisodeSection(
    groups: List<EpisodeGroup>,
    selectedGroupIndex: Int,
    episodes: List<Episode>,
    isLoadingEpisodes: Boolean,
    sniffingUrl: String?,
    onGroupSelected: (Int, EpisodeGroup) -> Unit,
    onEpisodeClick: (Episode) -> Unit
) {
    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedGroupIndex,
            backgroundColor = Color.Transparent,
            contentColor = NetflixRed,
            edgePadding = 0.dp,
            divider = {}
        ) {
            groups.forEachIndexed { index, group ->
                Tab(
                    selected = selectedGroupIndex == index,
                    onClick = { onGroupSelected(index, group) },
                    text = {
                        Text(
                            text = group.title,
                            color = if (selectedGroupIndex == index) Color.White else OnSurfaceMuted,
                            fontWeight = if (selectedGroupIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoadingEpisodes) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    Box(modifier = Modifier.size(60.dp).shimmerEffect())
                }
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                episodes.forEach { episode ->
                    EpisodeItem(
                        episode = episode,
                        isSniffing = sniffingUrl == episode.url,
                        onClick = { onEpisodeClick(episode) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(episode: Episode, isSniffing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 50.dp)
            .background(
                if (isSniffing) NetflixRed.copy(alpha = 0.3f) else SurfaceVariant,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSniffing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = NetflixRed,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = episode.title.replace("Tập ", ""),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

