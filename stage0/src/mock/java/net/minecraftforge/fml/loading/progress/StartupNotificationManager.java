package net.minecraftforge.fml.loading.progress;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Shim of FML's earlydisplay message handler.
 * <p>
 * This class exists in versions <=1.19.x as {@link StartupMessageManager}.
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
