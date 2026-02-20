package com.ipanda.domain.repository

import com.ipanda.domain.Episode
import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory

interface MovieRepository {
    suspend fun getHotMovies(): List<Movie>
    suspend fun getCategorizedHotMovies(): List<MovieCategory>
    suspend fun getMovieDetail(url: String): Movie?
}
