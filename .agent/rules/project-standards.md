---
trigger: always_on
---

# Movie Crawler Project Rules (Multi-Module Logic)

## 1. Module Responsibilities

- **`:shared` (KMP):** - Pure Business Logic: Data classes (`Movie`, `StreamSource`) and Repository interfaces.
  - No UI dependencies. No heavy extraction logic.
- **`:movie-crawler` (JVM/Java 21):**
  - The "Engine": Contains Jsoup implementations for scraping and Ktor/Browserless logic for sniffing.
  - Uses Java 21 Virtual Threads for high-concurrency extraction tasks.
- **`:desktopApp` (Compose):**
  - The "View": Strictly UI code that consumes the `:shared` and `:movie-crawler` modules.

## 2. Extraction Strategy

- **Isolation:** The UI must never call Browserless directly. It must go through the `:movie-crawler` module.
- **Jsoup Usage:** Perform all static HTML parsing within the `:movie-crawler` module to keep the `:shared` module platform-agnostic.
- **Browserless Sniffing:** Send JavaScript snippets to the Browserless REST API from the crawler module to intercept `.m3u8`/`.mpd` URLs.

## 3. Tech Stack

- **Shared:** Kotlin Serialization.
- **Crawler:** Java 21, Jsoup, Ktor Client (CIO engine).
- **Desktop:** Compose Multiplatform (Desktop target).

## 4. UI/UX Standards

- **Aesthetics:** When implementing or updating the UI, always reference major streaming platforms like **Netflix** or **FPT Play**.
- **Dark Mode:** Prioritize a premium dark theme with high contrast for cinematic experience.
- **Animations:** Use shimmer effects for loading states and smooth transitions (hero banners, card hover/click) to enhance user engagement.
- **Visuals:** Use gradient overlays on images (backdrops, posters) to make text readable and improve depth.
- **Micro-interactions:** Implement subtle feedback for user actions (playing, adding to list, selecting episodes).
