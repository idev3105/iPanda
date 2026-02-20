export default async ({ page }) => {
    try {
        await page.goto('__TARGET_URL__', { waitUntil: 'networkidle2', timeout: 60000 });

        // Wait up to 10s for any iframe to appear
        try {
            await page.waitForSelector('iframe', { timeout: 10000 });
        } catch (e) {
            // Proceed to evaluate even if timeout, maybe it's already there or handled by script
        }

        const iframeSrc = await page.evaluate(() => {
            // 1. Try to find the specific variable seen in the source (Site-specific optimization)
            // The user provided example uses 'player_aaaa'
            if (window.player_aaaa && window.player_aaaa.url) {
                return window.player_aaaa.url;
            }

            // 2. Generic Iframe Search
            const iframes = Array.from(document.querySelectorAll('iframe'));
            for (const iframe of iframes) {
                const src = iframe.src;
                if (!src) continue;

                // Skip common non-player iframes
                if (src.includes('cloudflare') || src.includes('recaptcha') || src.includes('google') || src.includes('chat')) {
                    continue;
                }

                // Check size to distinguish player from tracking pixel or small ads
                const rect = iframe.getBoundingClientRect();
                if (rect.width > 300 && rect.height > 200) {
                    return src;
                }
            }
            
            // Fallback: return the first iframe with a valid generic src if no "large" one found
            const firstValid = iframes.find(i => i.src && !i.src.includes('cloudflare'));
            return firstValid ? firstValid.src : null;
        });

        return iframeSrc;

    } catch (error) {
        return null; // Return null on failure
    }
};
