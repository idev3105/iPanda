package com.ipanda.domain.repository

import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.Episode

interface MovieCrawler {
    suspend fun crawlHotMovies(): List<Movie>
    suspend fun crawlCategorizedHotMovies(): List<MovieCategory>
    suspend fun crawlMovieDetails(url: String): Movie?
    suspend fun extractEpisodesFromEpisodeGroupUrl(url: String): List<Episode>
}
