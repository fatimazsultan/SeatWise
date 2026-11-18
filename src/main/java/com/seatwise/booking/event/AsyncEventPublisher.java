package com.seatwise.booking.event;

import com.seatwise.common.Guard;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Delivers booking events on a background executor so notification I/O does not sit
 * on the booking thread. If the executor is shut down, delivery falls back to the caller.
 */
public final class AsyncEventPublisher implements BookingEventPublisher, AutoCloseable {

    private static final Logger LOG = Logger.getLogger(AsyncEventPublisher.class.getName());
    private static final long SHUTDOWN_WAIT_SECONDS = 5;

    private final BookingEventPublisher delegate;
    private final ExecutorService executor;

    public AsyncEventPublisher(BookingEventPublisher delegate, ExecutorService executor) {
        this.delegate = Guard.notNull(delegate, "delegate");
        this.executor = Guard.notNull(executor, "executor");
    }

    @Override
    public void subscribe(BookingEventListener listener) {
        delegate.subscribe(listener);
    }

    @Override
    public void unsubscribe(BookingEventListener listener) {
        delegate.unsubscribe(listener);
    }

    @Override
    public void publish(BookingEvent event) {
        Guard.notNull(event, "event");
        try {
            executor.execute(() -> delegate.publish(event));
        } catch (RejectedExecutionException e) {
            LOG.log(Level.WARNING, "Async publisher rejected task; delivering inline", e);
            delegate.publish(event);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
