package com.ipanda.domain

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: String,
    val title: String,
    val url: String,
    val year: Int,
    val posterUrl: String,
    val plot: String = "",
    val description: String = "",
    val viewCount: String = "",
    val episodeGroups: List<EpisodeGroup> = emptyList()
)
