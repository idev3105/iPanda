package com.ipanda.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ipanda.android.screen.MovieDetailScreen
import com.ipanda.android.screen.MovieListScreen
import com.ipanda.android.screen.PlayerScreen
import com.ipanda.crawler.MovieCrawler
import com.ipanda.crawler.StreamSniffer
import com.ipanda.data.repository.MovieRepositoryImpl
import com.ipanda.data.repository.StreamRepositoryImpl
import com.ipanda.domain.StreamSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.ipanda.data.local.getDatabaseBuilder
import com.ipanda.data.local.getRoomDatabase
import com.ipanda.data.repository.FavoriteRepositoryImpl
import com.ipanda.domain.repository.FavoriteRepository

import androidx.compose.runtime.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ipanda.android.ui.NetflixRed
import com.ipanda.android.ui.SurfaceVariant
import com.ipanda.android.ui.OnSurfaceMuted
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize KMM cache directory
        com.ipanda.util.androidCacheDir = cacheDir
        com.ipanda.util.androidContext = this
        
        val prefs = getSharedPreferences("com.ipanda.android.settings", android.content.Context.MODE_PRIVATE)
        val defaultKey = BuildConfig.BROWSERLESS_API_KEY
        val defaultEndpoint = BuildConfig.BROWSERLESS_ENDPOINT
        
        val streamSniffer = StreamSniffer(
            browserlessApiKey = prefs.getString("BROWSERLESS_KEY", "")?.takeIf { it.isNotBlank() } ?: defaultKey,
            browserlessEndpoint = prefs.getString("BROWSERLESS_ENDPOINT", "")?.takeIf { it.isNotBlank() } ?: defaultEndpoint
        )

        val movieRepository = MovieRepositoryImpl(MovieCrawler())
        val streamRepository = StreamRepositoryImpl(streamSniffer)
        
        val database = com.ipanda.data.local.getRoomDatabase(com.ipanda.data.local.getDatabaseBuilder())
        val favoriteRepository = com.ipanda.data.repository.FavoriteRepositoryImpl(database)

        setContent {
            com.ipanda.android.ui.iPandaTheme {
                val navController = rememberNavController()
                var showSettingsDialog by remember { mutableStateOf(false) }

            if (showSettingsDialog) {
                SettingsDialog(
                    prefs = prefs,
                    defaultKey = defaultKey,
                    defaultEndpoint = defaultEndpoint,
                    onDismiss = { showSettingsDialog = false },
                    onSave = { apiKey, endpoint ->
                        val finalKey = apiKey.takeIf { it.isNotBlank() } ?: defaultKey
                        val finalEndpoint = endpoint.takeIf { it.isNotBlank() } ?: defaultEndpoint
                        
                        prefs.edit()
                            .putString("BROWSERLESS_KEY", apiKey)
                            .putString("BROWSERLESS_ENDPOINT", endpoint)
                            .apply()
                        
                        streamSniffer.browserlessApiKey = finalKey
                        streamSniffer.browserlessEndpoint = finalEndpoint
                        showSettingsDialog = false
                    },
                    onClearCache = {
                        streamRepository.clearCache()
                    }
                )
            }

            NavHost(navController = navController, startDestination = "movie_list") {
                composable("movie_list") {
                    MovieListScreen(
                        movieRepository = movieRepository,
                        favoriteRepository = favoriteRepository,
                        onMovieClick = { url ->
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            navController.navigate("movie_detail/$encodedUrl")
                        },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                
                composable(
                    "movie_detail/{movieUrl}",
                    arguments = listOf(navArgument("movieUrl") { type = NavType.StringType })
                ) { backStackEntry ->
                    val movieUrl = backStackEntry.arguments?.getString("movieUrl")?.let { 
                        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                    } ?: ""
                    
                    MovieDetailScreen(
                        movieUrl = movieUrl,
                        movieRepository = movieRepository,
                        streamRepository = streamRepository,
                        favoriteRepository = favoriteRepository,
                        onBack = { navController.popBackStack() },
                        onPlayClick = { streams, title ->
                            val streamsJson = Json.encodeToString(streams)
                            val encodedStreams = URLEncoder.encode(streamsJson, StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                            navController.navigate("player/$encodedStreams/$encodedTitle")
                        }
                    )
                }

                composable(
                    "player/{streamsJson}/{title}",
                    arguments = listOf(
                        navArgument("streamsJson") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val streamsJson = backStackEntry.arguments?.getString("streamsJson")?.let {
                        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                    } ?: "[]"
                    val title = backStackEntry.arguments?.getString("title")?.let {
                        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                    } ?: ""
                    
                    val streams = Json.decodeFromString<List<StreamSource>>(streamsJson)

                    PlayerScreen(
                        streamSources = streams,
                        episodeTitle = title,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            }
        }
    }
}
@Composable
fun SettingsDialog(
    prefs: android.content.SharedPreferences,
    defaultKey: String,
    defaultEndpoint: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onClearCache: suspend () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(prefs.getString("BROWSERLESS_KEY", "") ?: "") }
    var endpointInput by remember { mutableStateOf(prefs.getString("BROWSERLESS_ENDPOINT", "") ?: "") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceVariant,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cài đặt",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = NetflixRed
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OnSurfaceMuted)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Browserless Configuration",
                    style = MaterialTheme.typography.subtitle2,
                    color = OnSurfaceMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key") },
                    placeholder = { Text(defaultKey.take(10) + "...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = NetflixRed) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NetflixRed,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        textColor = Color.White,
                        cursorColor = NetflixRed,
                        focusedLabelColor = NetflixRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = { endpointInput = it },
                    label = { Text("Endpoint") },
                    placeholder = { Text(defaultEndpoint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = NetflixRed) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NetflixRed,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        textColor = Color.White,
                        cursorColor = NetflixRed,
                        focusedLabelColor = NetflixRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            onClearCache()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xóa Cache Stream", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSave(apiKeyInput, endpointInput) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = NetflixRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lưu thay đổi", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
