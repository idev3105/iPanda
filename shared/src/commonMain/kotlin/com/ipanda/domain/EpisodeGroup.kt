package com.ipanda.domain

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeGroup(
    val title: String,
    val episodes: List<Episode>,
    val url: String = ""
)
