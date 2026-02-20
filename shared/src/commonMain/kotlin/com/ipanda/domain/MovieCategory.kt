package com.ipanda.domain

import kotlinx.serialization.Serializable

@Serializable
data class MovieCategory(
    val title: String,
    val movies: List<Movie>
)
