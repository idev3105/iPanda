package com.ipanda.data.repository

import com.ipanda.data.local.AppDatabase
import com.ipanda.data.local.toDomain
import com.ipanda.data.local.toEntity
import com.ipanda.domain.Movie
import com.ipanda.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl(private val database: AppDatabase) : FavoriteRepository {
    override suspend fun addFavorite(movie: Movie) {
        database.favoriteMovieDao().insert(movie.toEntity().copy(timestamp = com.ipanda.util.currentTimeMillis()))
    }

    override suspend fun removeFavorite(movie: Movie) {
        database.favoriteMovieDao().delete(movie.toEntity())
    }

    override fun getAllFavorites(): Flow<List<Movie>> {
        return database.favoriteMovieDao().getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun isFavorite(movieId: String): Flow<Boolean> {
        return database.favoriteMovieDao().isFavorite(movieId)
    }
}
