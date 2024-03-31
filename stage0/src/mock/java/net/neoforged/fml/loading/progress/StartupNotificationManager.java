package net.neoforged.fml.loading.progress;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Same as {@link net.minecraftforge.fml.loading.progress.StartupNotificationManager}, but for NeoForge's FML.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class StartupNotificationManager {
    public static Optional<Consumer<String>> modLoaderConsumer() {
        throw new LinkageError("no lol");
    }

    public static Optional<Consumer<String>> locatorConsumer() {
        throw new LinkageError("no lol");
    }
}
