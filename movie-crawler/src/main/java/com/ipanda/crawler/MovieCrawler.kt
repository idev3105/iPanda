package com.ipanda.crawler

import com.ipanda.domain.Episode
import com.ipanda.domain.Movie
import com.ipanda.domain.MovieCategory
import com.ipanda.domain.repository.MovieCrawler as IMovieCrawler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
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
            applyPageScripts(doc)

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

            val playlist = doc.select("#playlist1").first()
            val episodes = playlist?.let { parseEpisodes(it, url) } ?: emptyList()

            Movie(
                id = id,
                title = title,
                url = url,
                year = year,
                posterUrl = posterUrl,
                plot = plot,
                description = description,
                viewCount = viewCount,
                episodes = episodes,
                seasons = extractSeasons(doc, url)
            )
        } catch (e: Exception) {
            logger.error(e) { "Error crawling movie details from $url" }
            null
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private fun applyPageScripts(doc: org.jsoup.nodes.Document) {
        // Script 1: Server titles
        doc.select(".server-title").forEach { element ->
            val text = element.text()
            val cleanedText = text.replace(Regex("\\s*\\(.*?\\)\\s*"), "")
            element.text(cleanedText)
        }

        // Script 2: Episode links (Related parts/seasons)
        val episodeLinks = doc.select(".streaming-server.btn-link-backup.btn-episode.black.episode-link")
        val listEpisode = doc.selectFirst(".list-episode") ?: return

        val outsideTitles = episodeLinks.map {
            it.text().replace(Regex("\\(.*?\\)"), "").trim()
        }.filter { it.isNotEmpty() }.toSet()

        if (episodeLinks.isEmpty() || episodeLinks.size == 10 || outsideTitles.size >= 10) {
            listEpisode.html("<li class=\"episode\"><a href=\"#\" class=\"streaming-server btn-link-backup btn-episode black episode-link\">Không có phần phim liên quan</a></li>")
        } else {
            val regex = Regex("\\((.*?)\\)")
            val pageTitle = doc.title()
            val activeTitle = regex.find(pageTitle)?.groupValues?.get(1) ?: ""

            data class LinkInfo(val element: org.jsoup.nodes.Element, val extractedTitle: String)

            val linkInfos = episodeLinks.map { element ->
                val title = regex.find(element.text())?.groupValues?.get(1) ?: ""
                LinkInfo(element, title)
            }

            val partLinks = linkInfos.filter { it.extractedTitle.matches(Regex("^Phần \\d+$")) }
                .sortedBy { it.extractedTitle.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val ovaLinks = linkInfos.filter { it.extractedTitle.matches(Regex("^OVA \\d+$")) }
                .sortedBy { it.extractedTitle.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val otherLinks = linkInfos.filter { info ->
                !info.extractedTitle.matches(Regex("^Phần \\d+$")) && !info.extractedTitle.matches(Regex("^OVA \\d+$"))
            }

            val sortedLinks = partLinks + ovaLinks + otherLinks

            listEpisode.empty()
            sortedLinks.forEach { info ->
                info.element.text(info.extractedTitle)
                if (info.extractedTitle.isNotEmpty() && info.extractedTitle == activeTitle) {
                    info.element.addClass("active")
                }
                val li = doc.createElement("li").addClass("episode")
                li.appendChild(info.element)
                listEpisode.appendChild(li)
            }
        }
    }

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

    private suspend fun extractSeasons(doc: Document, baseUrl: String): List<Movie> = coroutineScope {
        val rawSeasonUrls = doc.select(".list-episode a")
            .map { it.attr("href") }
            .filter { it.isNotBlank() && it != "#" }
        
        logger.debug { "Raw season URLs: $rawSeasonUrls" }

        val seasonUrls = rawSeasonUrls
            .map { resolveUrl(baseUrl, it) }
            .filter { it != baseUrl }
            .distinct()
        
        logger.debug { "Resolved season URLs: $seasonUrls" }

        seasonUrls.map { url ->
            async(Dispatchers.IO) {
                try {
                    val sDoc = fetcher.fetch(url)
                    val title = sDoc.selectFirst("h1.title")
                        ?.text()?.substringBefore("lượt xem")?.trim().orEmpty()
                    
                    val thumb = sDoc.selectFirst(".myui-vodlist__thumb img") ?: sDoc.selectFirst(".myui-content__thumb img")
                    val posterUrl = thumb?.let {
                        it.attr("data-original").ifEmpty { it.attr("src") }
                    }.orEmpty()

                    Movie(
                        id = extractIdFromUrl(url),
                        title = title,
                        url = url,
                        year = 0,
                        posterUrl = posterUrl
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
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
