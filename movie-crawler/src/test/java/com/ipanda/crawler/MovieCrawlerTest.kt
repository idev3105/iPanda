package com.ipanda.crawler

import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.Episode
import com.ipanda.domain.EpisodeGroup
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
            assertTrue(movie?.episodeGroups?.isNotEmpty() == true)
            
            movie?.episodeGroups?.forEach { group ->
                println("Server: ${group.title}, Episodes: ${group.episodes.size}")
            }
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
            val hasPhan1 = movie?.episodeGroups?.any { group -> 
                group.title.contains("Phần 1")
            } == true
            assertTrue(hasPhan1, "Should have extracted 'Phần 1' from .list-episode")
        }
    }

    @Test
    fun `extractEpisodesFromEpisodeGroupUrl returns episodes from playlist`() {
        runBlocking {
            val crawler = MovieCrawler()
            val url = "https://hhhtq.team/phim/115/"
            val episodes = crawler.extractEpisodesFromEpisodeGroupUrl(url)

            println("Episodes from $url:")
            episodes.forEach { println("  - ${it.title} -> ${it.url}") }

            assertTrue(episodes.isNotEmpty(), "Should find episodes in #playlist1")
            
            episodes.forEach { episode ->
                assertTrue(episode.title.isNotBlank(), "Episode title should not be blank")
                assertTrue(episode.url.startsWith("https://hhhtq.team/"), "Episode URL should be absolute: ${episode.url}")
            }

            // Verify specific episode exists
            val hasTap1 = episodes.any { it.title.contains("Tập 1") }
            assertTrue(hasTap1, "Should contain 'Tập 1'")
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

