package com.ipanda.desktop.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipanda.desktop.component.AsyncImage
import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.repository.MovieRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun MovieListScreen(
    movieRepository: MovieRepository,
    favoriteRepository: com.ipanda.domain.repository.FavoriteRepository,
    onMovieClick: (Movie) -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var categorizedMovies by remember { mutableStateOf<List<MovieCategory>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    movies = movieRepository.getHotMovies()
                    categorizedMovies = movieRepository.getCategorizedHotMovies()
                }
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        favoriteRepository.getAllFavorites().collect { favs ->
            favorites = favs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🐼 iPanda",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Phim Hot",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Content ──
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌ Lỗi tải phim", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        movies = movieRepository.getHotMovies()
                                        categorizedMovies = movieRepository.getCategorizedHotMovies()
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                }
                                isLoading = false
                            }
                        }) {
                            Text("Thử lại")
                        }
                    }
                }
                movies.isEmpty() -> {
                    Text(
                        "Không có phim nào",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (favorites.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "❤️ Phim Yêu Thích",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                                )
                            }
                            items(favorites, key = { "fav_${it.id}" }) { movie ->
                                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                Text(
                                    "🔥 Phim Hot",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                        }
                        
                        items(movies, key = { it.id }) { movie ->
                            MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                        }

                        // ── Categorized Sections ──
                        categorizedMovies.forEach { category ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            items(category.movies, key = { "${category.title}_${it.id}" }) { movie ->
                                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f)

    Card(
        modifier = Modifier
            .width(180.dp)
            .scale(scale)
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHovered) 12.dp else 4.dp
        )
    ) {
        Box {
            // Poster
            AsyncImage(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Title overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (movie.year > 0) {
                    Text(
                        text = "${movie.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
