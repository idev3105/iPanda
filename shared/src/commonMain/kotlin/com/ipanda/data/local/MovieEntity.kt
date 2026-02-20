package com.ipanda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ipanda.domain.Movie

@Entity(tableName = "favorite_movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val year: Int,
    val posterUrl: String,
    val plot: String,
    val description: String,
    val viewCount: String,
    val timestamp: Long = 0 // For sorting by recently added
)

fun Movie.toEntity() = MovieEntity(
    id = id,
    title = title,
    url = url,
    year = year,
    posterUrl = posterUrl,
    plot = plot,
    description = description,
    viewCount = viewCount,
    timestamp = 0 // Should be set to current time when saving
)

fun MovieEntity.toDomain() = Movie(
    id = id,
    title = title,
    url = url,
    year = year,
    posterUrl = posterUrl,
    plot = plot,
    description = description,
    viewCount = viewCount
)
