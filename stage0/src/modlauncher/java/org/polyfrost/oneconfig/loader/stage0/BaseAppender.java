package org.polyfrost.oneconfig.loader.stage0;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.function.Consumer;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class BaseAppender extends AbstractAppender {
    private final Consumer<String> consumer;

    protected BaseAppender(
            String name,
            Consumer<String> consumer
    ) {
        super(
                "oneconfig-loader-" + name,
                null,
                null,
                true,
                Property.EMPTY_ARRAY
        );
        this.consumer = consumer;
    }

    @Override
    public void append(LogEvent event) {
        this.consumer.accept("OC: " + event.getMessage().getFormattedMessage());
    }
}
