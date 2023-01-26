package cc.polyfrost.oneconfig.loader.utils;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * This probably could be its own method, but I like class separations.
 *
 * @author xtrm
 */
@Log4j2
public class DiscordProvider {
    private static final String DISCORD_FETCH_URL =
            "https://data.woverflow.cc/discord.txt";
    private static final String DISCORD_FALLBACK_URL =
            "https://inv.wtf/polyfrost";

    @Nullable
    private static String cachedUrl = null;

    private DiscordProvider() {
        throw new IllegalStateException("This class cannot be instantiated.");
    }

    private static String fetchDiscordURL() throws IOException {
        URL url = new URL(DISCORD_FETCH_URL);
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = IOUtils.readAllBytes(inputStream);
            return new String(bytes, StandardCharsets.UTF_8)
                    .replace("\n", "")
                    .trim();
        }
    }

    @NotNull
    public static String getDiscordURL() {
        try {
            if (cachedUrl == null) {
                cachedUrl = fetchDiscordURL();
            }
            return cachedUrl;
        } catch (IOException e) {
            // Only log, don't call ErrorHandler since the Discord URL is not
            // critical and is probably requested from there anyway.
            log.error("Failed to fetch Discord URL", e);

            // Reset the cache and try again next time.
            cachedUrl = null;
            return DISCORD_FALLBACK_URL;
        }
    }
}
