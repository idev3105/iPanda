export default async ({ page }) => {
    const SUPPORTED_EXTENSIONS = /\.(m3u8|mp4|mpd|mov|m4v)($|\?)/i;
    const IGNORE_PATTERNS = /(dmxleo|favicon|logo|css|js)/i;
    const STREAM_CONTENT_TYPES = /(mpegurl|video\/mp4|dash\+xml|video\/quicktime)/i;

    const streams = new Map();
    
    // Listen for responses to capture stream URLs
    page.on('response', response => {
        try {
            const url = response.url();
            const status = response.status();
            
            if (status >= 200 && status < 400) {
                const headers = response.headers();
                const contentType = headers['content-type'] || '';
                
                const isStreamUrl = SUPPORTED_EXTENSIONS.test(url);
                const isStreamType = STREAM_CONTENT_TYPES.test(contentType);

                if ((isStreamUrl || isStreamType) && !IGNORE_PATTERNS.test(url)) {
                    streams.set(url, {
                        url: url,
                        headers: response.request().headers()
                    });
                }
            }
        } catch (e) {
            // Handle transient errors (e.g. response closed)
        }
    });

    try {
        // Navigate to the target URL
        await page.goto('__TARGET_URL__', { 
            waitUntil: 'networkidle2', 
            timeout: 20000 
        });

        // Run playback trigger logic in the background so it doesn't block the main wait period
        (async () => {
            const selectors = [
                '#app > div > div.embed__wrapper > div > button',
                '#app > div > div.embed__wrapper > div > button > span',
                '.vjs-big-play-button',
                'button.play'
            ];
            
            selectors.forEach(async (selector) => {
                try {
                    await page.waitForSelector(selector, { visible: true, timeout: 5000 });
                    await page.click(selector);
                } catch (e) {}
            });
        })();
    } catch (e) {
        // Continue even if navigation fails
    }

    // Wait for 10 seconds to allow for dynamic stream loading/sniffing
    await new Promise(resolve => setTimeout(resolve, 10000));
    
    return Array.from(streams.values());
};

