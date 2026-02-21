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
import kotlinx.serialization.json.Json
import java.net.URI


private val logger = KotlinLogging.logger {}

interface PageFetcher {
    fun fetch(url: String): Document
}

class JsoupPageFetcher : PageFetcher {
    override fun fetch(url: String): Document = Jsoup.connect(url).get()
}

class MovieCrawler(
    private val config: CrawlerConfig,
    private val fetcher: PageFetcher = JsoupPageFetcher()
) : IMovieCrawler {

    constructor() : this(loadDefaultConfig())

    companion object {
        private fun loadDefaultConfig(): CrawlerConfig {
            val content = MovieCrawler::class.java.getResourceAsStream("/scrapers/hhhtq.json")
                ?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Default config not found")
            return Json { ignoreUnknownKeys = true }.decodeFromString<CrawlerConfig>(content)
        }
    }


    // ── Public API ───────────────────────────────────────────────────────
    override suspend fun crawlHotMovies(): List<Movie> {
        logger.info { "Crawling hot movies from ${config.siteName}..." }
        val doc = fetcher.fetch("${config.baseUrl}/")
        val hotSection = doc.select(config.selectors.hotSection).first() ?: return emptyList()
        val movieElements = hotSection.select(config.selectors.hotMovieBox)
        logger.debug { "Found ${movieElements.size} movie elements in hot section" }
        return movieElements.mapNotNull { parseMovie(it) }
    }

    override suspend fun crawlCategorizedHotMovies(): List<MovieCategory> {
        logger.info { "Crawling categorized hot movies from ${config.siteName}..." }
        val doc = fetcher.fetch("${config.baseUrl}/")
        val panels = doc.select(config.selectors.categoryPanel)
        logger.debug { "Found ${panels.size} category panels" }
        return panels.mapNotNull { panel ->
            val title = panel.select(config.selectors.categoryTitle).text().trim()
            if (title.isEmpty()) return@mapNotNull null

            val movieElements = panel.select(config.selectors.categoryMovieBox)
            val movies = movieElements.mapNotNull { parseMovie(it) }

            if (movies.isEmpty()) null else MovieCategory(title, movies)
        }
    }


    override suspend fun crawlMovieDetails(url: String): Movie? {
        logger.info { "Crawling movie details from $url" }
        return try {
            val doc = fetcher.fetch(url)
            applyPageScripts(doc)

            val titleElement = doc.selectFirst(config.selectors.detailTitle)
            val title = if (config.selectors.detailTitleCleanup != null) {
                titleElement?.text()?.substringBefore(config.selectors.detailTitleCleanup)?.trim().orEmpty()
            } else {
                titleElement?.text()?.trim().orEmpty()
            }

            val description = doc.select(config.selectors.detailDescription)
                .text().trim()

            val viewCount = doc.select(config.selectors.detailViewCount).text().trim()

            val plot = doc.select(config.selectors.detailPlot)
                .firstOrNull { it.text().contains(config.selectors.detailPlotMarker) }
                ?.text()?.substringAfter(":")?.trim()
                ?: description

            var posterUrl = ""
            for (selector in config.selectors.detailThumbs) {
                val thumb = doc.selectFirst(selector)
                if (thumb != null) {
                    posterUrl = thumb.attr(config.selectors.itemPosterAttr).ifEmpty { thumb.attr("src") }
                    if (posterUrl.isNotEmpty()) break
                }
            }

            val year = doc.select(config.selectors.detailPlot)
                .firstOrNull { it.text().contains(config.selectors.detailYearMarker) }
                ?.text()?.filter { it.isDigit() }
                ?.toIntOrNull() ?: 0

            val id = extractIdFromUrl(url)

            val playlist = doc.select(config.selectors.detailPlaylist).first()
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
            val thumbLink = element.selectFirst(config.selectors.itemThumb) ?: return null
            val title = thumbLink.attr(config.selectors.itemTitleAttr)
            val relativeUrl = thumbLink.attr(config.selectors.itemUrlAttr)
            val id = extractIdFromUrl(relativeUrl)
            val posterUrl = thumbLink.attr(config.selectors.itemPosterAttr).ifEmpty {
                extractUrlFromStyle(thumbLink.attr("style"))
            }

            Movie(
                id = id,
                title = title,
                url = resolveUrl("${config.baseUrl}/", relativeUrl),
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
        val rawSeasonUrls = doc.select(config.selectors.seasonLink)
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
                    val titleElement = sDoc.selectFirst(config.selectors.detailTitle)
                    val title = if (config.selectors.detailTitleCleanup != null) {
                        titleElement?.text()?.substringBefore(config.selectors.detailTitleCleanup)?.trim().orEmpty()
                    } else {
                        titleElement?.text()?.trim().orEmpty()
                    }
                    
                    var posterUrl = ""
                    for (selector in config.selectors.detailThumbs) {
                        val thumb = sDoc.selectFirst(selector)
                        if (thumb != null) {
                            posterUrl = thumb.attr(config.selectors.itemPosterAttr).ifEmpty { thumb.attr("src") }
                            if (posterUrl.isNotEmpty()) break
                        }
                    }

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
        container.select(config.selectors.episodeList).map { a ->
            val title = config.selectors.episodeTitleInner?.let { a.select(it).text().trim() } 
                ?: a.text().trim()
            
            Episode(
                title = title.ifEmpty { a.text().trim() },
                url = resolveUrl(baseUrl, a.attr(config.selectors.episodeUrlAttr))
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
