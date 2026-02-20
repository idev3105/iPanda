package com.ipanda.data.repository

import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.Episode
import com.ipanda.domain.repository.MovieRepository
import com.ipanda.domain.repository.MovieCrawler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepositoryImpl(
    private val crawler: MovieCrawler
) : MovieRepository {

    override suspend fun getHotMovies(): List<Movie> = withContext(Dispatchers.IO) {
        crawler.crawlHotMovies()
    }

    override suspend fun getCategorizedHotMovies(): List<MovieCategory> = withContext(Dispatchers.IO) {
        crawler.crawlCategorizedHotMovies()
    }

    override suspend fun getMovieDetail(url: String): Movie? = withContext(Dispatchers.IO) {
        crawler.crawlMovieDetails(url)
    }
}
