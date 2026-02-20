package com.ipanda.domain.repository

import com.ipanda.domain.Movie
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    suspend fun addFavorite(movie: Movie)
    suspend fun removeFavorite(movie: Movie)
    fun getAllFavorites(): Flow<List<Movie>>
    fun isFavorite(movieId: String): Flow<Boolean>
}
