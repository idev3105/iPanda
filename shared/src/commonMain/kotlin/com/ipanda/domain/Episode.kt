package com.ipanda.domain

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val title: String,
    val url: String
)
