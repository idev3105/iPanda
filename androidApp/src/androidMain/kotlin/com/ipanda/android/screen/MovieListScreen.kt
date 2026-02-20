package com.ipanda.android.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ipanda.android.ui.BackgroundDark
import com.ipanda.android.ui.NetflixRed
import com.ipanda.android.ui.shimmerEffect
import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MovieListScreen(
    movieRepository: MovieRepository,
    favoriteRepository: com.ipanda.domain.repository.FavoriteRepository,
    onMovieClick: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var categorizedMovies by remember { mutableStateOf<List<MovieCategory>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    var isTopBarVisible by remember { mutableStateOf(true) }
    var previousScrollValue by remember { mutableStateOf(0) }

    LaunchedEffect(scrollState.value) {
        val currentScrollValue = scrollState.value
        if (currentScrollValue > previousScrollValue && currentScrollValue > 100) {
            isTopBarVisible = false
        } else if (currentScrollValue < previousScrollValue || currentScrollValue < 10) {
            isTopBarVisible = true
        }
        previousScrollValue = currentScrollValue
    }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val hot = movieRepository.getHotMovies()
                val categorized = movieRepository.getCategorizedHotMovies()
                movies = hot
                categorizedMovies = categorized
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        favoriteRepository.getAllFavorites().collect { favs ->
            favorites = favs
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background)) {
        if (isLoading) {
            LoadingSkeleton()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp)
            ) {
                // Improved Hero Slider (Carousel with Auto-slide & Peek)
                if (movies.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { movies.size })

                    // Auto-sliding logic with reset on interaction
                    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                        if (!pagerState.isScrollInProgress) {
                            kotlinx.coroutines.delay(5000) // Wait 5s after interaction stops or page changes
                            if (pagerState.pageCount > 0) {
                                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    }

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)) {
                        // Background Images with Pager
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            // Scale animation for focal effect
                            val pageOffset = (pagerState.currentPage - page).coerceIn(-1, 1)
                            val scale = 1f - (kotlin.math.abs(pageOffset) * 0.1f)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onMovieClick(movies[page].url) }
                            ) {
                                AsyncImage(
                                    model = movies[page].posterUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Cinematic Gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.3f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // Detached Overlay (Title + Buttons)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 50.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated Title Transition
                            AnimatedContent(
                                targetState = movies[pagerState.currentPage],
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(500)) with fadeOut(
                                        animationSpec = tween(
                                            500
                                        )
                                    )
                                },
                                label = "title_transition"
                            ) { movie ->
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.h4,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { onMovieClick(movies[pagerState.currentPage].url) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Phát", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onMovieClick(movies[pagerState.currentPage].url) },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Gray.copy(
                                            alpha = 0.6f
                                        )
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Thông tin",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Favorite Movies Section
                if (favorites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Phim yêu thích",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favorites.size) { index ->
                            val movie = favorites[index]
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.url) },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }

                // Categorized Movies Sections
                categorizedMovies.forEach { category ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(category.movies.size) { index ->
                            val movie = category.movies[index]
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.url) },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Search/Settings Top Bar Overlay
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            TransparentTopBar(onOpenSettings = onOpenSettings)
        }
    }
}


@Composable
fun MovieCard(movie: Movie, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(0.66f)
            .clickable(onClick = onClick),
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TransparentTopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo-like text
        Text(
            text = "iPanda",
            color = NetflixRed,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Black
        )

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
    }
}

@Composable
fun LoadingSkeleton() {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(BackgroundDark)) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .shimmerEffect())
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Box(modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.66f)
                    .shimmerEffect())
            }
        }
    }
}
