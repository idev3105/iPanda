package com.ipanda.crawler

import kotlinx.serialization.Serializable

@Serializable
data class CrawlerConfig(
    val siteName: String,
    val baseUrl: String,
    val selectors: SelectorsConfig
)

@Serializable
data class SelectorsConfig(
    // Hot movies on index page
    val hotSection: String,
    val hotMovieBox: String,

    // Categorized movies on index page
    val categoryPanel: String,
    val categoryTitle: String,
    val categoryMovieBox: String,

    // Movie item parsing
    val itemThumb: String,
    val itemTitleAttr: String = "title",
    val itemUrlAttr: String = "href",
    val itemPosterAttr: String = "data-original",

    // Movie details
    val detailTitle: String,
    val detailTitleCleanup: String? = null,
    val detailDescription: String,
    val detailViewCount: String,
    val detailPlot: String,
    val detailPlotMarker: String,
    val detailThumbs: List<String>,
    val detailYearMarker: String,
    val detailPlaylist: String,

    // Episodes
    val episodeList: String,
    val episodeTitleInner: String? = null,
    val episodeUrlAttr: String = "href",

    // Seasons
    val seasonList: String,
    val seasonLink: String
)
