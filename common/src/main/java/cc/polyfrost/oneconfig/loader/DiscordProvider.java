package cc.polyfrost.oneconfig.loader;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * This probably could be its own method, but I like class separations.
 *
 * @author xtrm
 */
public class DiscordProvider {
    private static final String DISCORD_FETCH_URL =
            "https://data.woverflow.cc/discord.txt";
    private static final String DISCORD_FALLBACK_URL =
            "";

    @Nullable
    private static String cachedUrl = null;

    private DiscordProvider() {
    }

    private static String fetchDiscordURL() throws IOException {
        URL url = new URL(DISCORD_FETCH_URL);
        byte[] bytes = IOUtils.toByteArray(url.openConnection());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @NotNull
    public static String getDiscordURL() {
        try {
            if (cachedUrl == null) {
                cachedUrl = fetchDiscordURL();
            }
            return cachedUrl;
        } catch (IOException e) {
            return DISCORD_FALLBACK_URL;
        }
    }
}
