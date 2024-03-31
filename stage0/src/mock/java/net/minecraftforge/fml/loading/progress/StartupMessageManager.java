package net.minecraftforge.fml.loading.progress;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Shim of FML's earlydisplay message handler.
 * <p>
 * This class was renamed after versions >=1.20.x to {@link StartupNotificationManager}.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class StartupMessageManager {
    public static Optional<Consumer<String>> modLoaderConsumer() {
        throw new LinkageError("no lol");
    }

    public static Optional<Consumer<String>> locatorConsumer() {
        throw new LinkageError("no lol");
    }
}
