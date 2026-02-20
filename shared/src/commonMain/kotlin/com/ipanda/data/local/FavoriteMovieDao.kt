package com.ipanda.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: MovieEntity)

    @Delete
    suspend fun delete(movie: MovieEntity)

    @Query("SELECT * FROM favorite_movies ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<MovieEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE id = :movieId)")
    fun isFavorite(movieId: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_movies WHERE id = :movieId")
    suspend fun getById(movieId: String): MovieEntity?
}
