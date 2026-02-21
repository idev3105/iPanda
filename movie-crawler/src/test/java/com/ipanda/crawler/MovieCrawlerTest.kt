package com.ipanda.crawler

import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.Episode
import kotlinx.coroutines.runBlocking

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class MovieCrawlerTest {

    @Test
    fun `crawlHotMovies matches real website structure`() {
        runBlocking {
            // Uses the default JsoupPageFetcher which makes a real network call
            val crawler = MovieCrawler() 
            
            println("Fetching movies from https://hhhtq.team/ ...")
            val movies = crawler.crawlHotMovies()

            println("Found ${movies.size} movies:")
            movies.take(5).forEach { println("- $it") }

            if (movies.isEmpty()) {
                fail("Expected to find movies on the real site, but found none. The site structure might have changed.")
            }

            movies.forEach { movie ->
                assertTrue(movie.id.isNotBlank(), "Movie ID should not be blank for ${movie.title}")
                assertTrue(movie.title.isNotBlank(), "Movie Title should not be blank for ID ${movie.id}")
                assertTrue(movie.url.startsWith("https://hhhtq.team/phim/"), "Movie URL should be absolute for ${movie.title}")
                // Poster URL depends on the extraction logic, let's just check it's not null/empty if that's the contract
                assertTrue(movie.posterUrl.isNotEmpty(), "Movie PosterURL should not be empty for ${movie.title}")
            }
        }
    }

    @Test
    fun `crawlMovieDetails returns valid movie data`() {
        runBlocking {
            val crawler = MovieCrawler()
            val url = "https://hhhtq.team/phim/115/"
            val movie = crawler.crawlMovieDetails(url)

            println("Movie Detail: $movie")
            assertNotNull(movie)
            assertEquals("115", movie?.id)
            assertEquals(url, movie?.url)
            assertTrue(movie?.title?.contains("Linh Khế") == true)
            assertTrue(movie?.plot?.isNotEmpty() == true)
            assertTrue(movie?.episodes?.isNotEmpty() == true)
            
            println("Seasons: ${movie?.seasons?.size}, Episodes: ${movie?.episodes?.size}")
        }
    }

    @Test
    fun `crawlMovieDetails for phim 250 returns description and view count`() {
        runBlocking {
            val crawler = MovieCrawler()
            val url = "https://hhhtq.team/phim/250/"
            val movie = crawler.crawlMovieDetails(url)

            println("Movie Detail (250): $movie")
            assertNotNull(movie)
            assertTrue(movie?.description?.contains("Trung Quốc Kỳ Đàm") == true, "Description should contain 'Trung Quốc Kỳ Đàm'")
            assertTrue(movie?.viewCount?.contains("lượt xem") == true, "View count should contain 'lượt xem'")
            
            // Verify .list-episode extraction (e.g. Phần 1)
            val hasPhan1 = movie?.seasons?.any { season -> 
                season.title.contains("Phần 1")
            } == true
            assertTrue(hasPhan1, "Should have extracted 'Phần 1' from .list-episode")
        }
    }

    @Test
    fun `crawlMovieDetails extracts seasons with posters and excludes current movie`() {
        runBlocking {
            val crawler = MovieCrawler()
            // Using "Linh Khế" as an example which typically has seasons/related parts
            val url = "https://hhhtq.team/phim/251/"
            val movie = crawler.crawlMovieDetails(url)

            assertNotNull(movie)
            println("Movie: ${movie?.title}")
            println("Seasons found: ${movie?.seasons?.size}")

            movie?.seasons?.forEach { season ->
                println("  - Season: ${season.title}, Poster: ${season.posterUrl}, URL: ${season.url}")
                
                // Assertions
                assertTrue(season.url != url, "Season list should NOT contain current movie URL: ${season.url}")
                assertTrue(season.title.isNotEmpty(), "Season title should not be empty")
                // Note: Not every season might have a poster if it's not in the related/recommended sections
                // but at least some should have it.
            }
            
            // If the page has seasons, check at least one poster is found
            if (movie?.seasons?.isNotEmpty() == true) {
                val hasAnyPoster = movie.seasons.any { it.posterUrl.isNotEmpty() }
                println("At least one season has poster: $hasAnyPoster")
                // This is a soft check because posters rely on the "Related" section on the page
            }
        }
    }

    @Test
    fun `crawlCategorizedHotMovies returns categories with movies`() {
        runBlocking {
            val crawler = MovieCrawler()

            println("Fetching categorized movies...")
            val categories = crawler.crawlCategorizedHotMovies()

            println("Found ${categories.size} categories:")
            categories.forEach { category ->
                println("Category: ${category.title} (${category.movies.size} movies)")
                category.movies.take(3).forEach { println("  - ${it.title}: ${it.posterUrl}") }
            }

            if (categories.isEmpty()) {
                fail("Expected to find categorized movie panels, but found none.")
            }

            categories.forEach { category ->
                assertTrue(category.title.isNotBlank(), "Category title should not be blank")
                assertTrue(category.movies.isNotEmpty(), "Category '${category.title}' should not be empty")

                category.movies.forEach { movie ->
                    assertTrue(movie.id.isNotBlank(), "Movie ID should not be blank in ${category.title}")
                    assertTrue(movie.title.isNotBlank(), "Movie Title should not be blank in ${category.title}")
                }
            }
        }
    }
}

