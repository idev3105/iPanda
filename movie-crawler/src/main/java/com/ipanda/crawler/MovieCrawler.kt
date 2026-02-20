package com.ipanda.crawler

import com.ipanda.domain.Episode
import com.ipanda.domain.EpisodeGroup
import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.repository.MovieCrawler as IMovieCrawler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

private val logger = KotlinLogging.logger {}

interface PageFetcher {
    fun fetch(url: String): Document
}

class JsoupPageFetcher : PageFetcher {
    override fun fetch(url: String): Document = Jsoup.connect(url).get()
}

class MovieCrawler(private val fetcher: PageFetcher = JsoupPageFetcher()) : IMovieCrawler {

    companion object {
        private const val BASE_URL = "https://hhhtq.team"
    }

    // ── Public API ───────────────────────────────────────────────────────
    override suspend fun crawlHotMovies(): List<Movie> {
        logger.info { "Crawling hot movies..." }
        val doc = fetcher.fetch("$BASE_URL/")
        val hotSection = doc.select("#index-hot").first() ?: return emptyList()
        val movieElements = hotSection.select(".myui-vodlist__box")
        logger.debug { "Found ${movieElements.size} movie elements in hot section" }
        return movieElements.mapNotNull { parseMovie(it) }
    }

    override suspend fun crawlCategorizedHotMovies(): List<MovieCategory> {
        logger.info { "Crawling categorized hot movies..." }
        val doc = fetcher.fetch("$BASE_URL/")
        val panels = doc.select(".myui-panel.myui-panel-bg")
        logger.debug { "Found ${panels.size} category panels" }
        return panels.mapNotNull { panel ->
            val title = panel.select("h3").text().trim()
            if (title.isEmpty()) return@mapNotNull null

            val movieElements = panel.select(".myui-vodlist__box")
            val movies = movieElements.mapNotNull { parseMovie(it) }

            if (movies.isEmpty()) null else MovieCategory(title, movies)
        }
    }

    override suspend fun crawlMovieDetails(url: String): Movie? {
        logger.info { "Crawling movie details from $url" }
        return try {
            val doc = fetcher.fetch(url)

            val title = doc.selectFirst("h1.title")
                ?.text()?.substringBefore("lượt xem")?.trim().orEmpty()

            val description = doc.select("#desc > div > div > div > span.sketch.content")
                .text().trim()

            val viewCount = doc.select(
                "body > div.container > div:nth-child(1) > div > div.myui-content__detail > h1:nth-child(3)"
            ).text().trim()

            val plot = doc.select(".myui-content__detail p")
                .firstOrNull { it.text().contains("Nội dung") }
                ?.text()?.substringAfter(":")?.trim()
                ?: description

            val thumb = doc.selectFirst(".myui-vodlist__thumb img") ?: doc.selectFirst(".myui-content__thumb img")
            val posterUrl = thumb?.let {
                it.attr("data-original").ifEmpty { it.attr("src") }
            }.orEmpty()

            val year = doc.select(".myui-content__detail p")
                .firstOrNull { it.text().contains("Năm") }
                ?.text()?.filter { it.isDigit() }
                ?.toIntOrNull() ?: 0

            val id = extractIdFromUrl(url)

            Movie(
                id = id,
                title = title,
                url = url,
                year = year,
                posterUrl = posterUrl,
                plot = plot,
                description = description,
                viewCount = viewCount,
                episodeGroups = extractEpisodeGroups(doc, url)
            )
        } catch (e: Exception) {
            logger.error(e) { "Error crawling movie details from $url" }
            null
        }
    }

    override suspend fun extractEpisodesFromEpisodeGroupUrl(url: String): List<Episode> {
        val doc = fetcher.fetch(url)
        val playlist = doc.select("#playlist1").first() ?: return emptyList()
        return parseEpisodes(playlist, url)
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private fun parseMovie(element: Element): Movie? {
        return try {
            val thumbLink = element.selectFirst(".myui-vodlist__thumb") ?: return null
            val title = thumbLink.attr("title")
            val relativeUrl = thumbLink.attr("href")
            val id = extractIdFromUrl(relativeUrl)
            val posterUrl = thumbLink.attr("data-original").ifEmpty {
                extractUrlFromStyle(thumbLink.attr("style"))
            }

            Movie(
                id = id,
                title = title,
                url = resolveUrl("$BASE_URL/", relativeUrl),
                year = 0,
                posterUrl = posterUrl,
                plot = ""
            )
        } catch (e: Exception) {
            logger.error(e) { "Error parsing movie element" }
            null
        }
    }

    private fun extractEpisodeGroups(doc: Document, baseUrl: String): List<EpisodeGroup> {
        val groups = mutableListOf<EpisodeGroup>()

        // Default group from current page
        val defaultTitle = doc.selectFirst(".title")?.text()?.substringBefore("lượt xem")?.trim().orEmpty()
        if (defaultTitle.isNotEmpty()) {
            groups.add(EpisodeGroup(title = defaultTitle, episodes = emptyList(), url = baseUrl))
        }

        // Seasons / Parts from .list-episode
        doc.select(".list-episode a:not([class*=active])").forEach { a ->
            val groupTitle = a.text().trim()
            val groupUrl = resolveUrl(baseUrl, a.attr("href"))
            if (groups.none { it.title == groupTitle }) {
                groups += EpisodeGroup(title = groupTitle, episodes = emptyList(), url = groupUrl)
            }
        }

        return groups.distinctBy { it.title + it.url }
    }

    private fun parseEpisodes(container: Element, baseUrl: String): List<Episode> =
        container.select("li a").map { a ->
            Episode(
                title = a.select("b").text().trim().ifEmpty { a.text().trim() },
                url = resolveUrl(baseUrl, a.attr("href"))
            )
        }

    private fun extractIdFromUrl(url: String): String =
        url.trim('/').split("/").lastOrNull().orEmpty()

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http")) return relativeUrl
        val uri = URI(baseUrl)
        return if (relativeUrl.startsWith("/")) {
            "${uri.scheme}://${uri.host}$relativeUrl"
        } else {
            baseUrl.substringBeforeLast("/") + "/" + relativeUrl
        }
    }

    private fun extractUrlFromStyle(style: String): String {
        val start = style.indexOf("url(") + 4
        val end = style.indexOf(")", start)
        return if (start >= 4 && end > start) style.substring(start, end).trim() else ""
    }
}
