package com.seatwise.booking;

import com.seatwise.common.Guard;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background job that releases expired holds. It runs on its own daemon thread while
 * customers book on other threads. A failing sweep is logged and retried; it does not
 * kill the scheduler.
 */
public final class HoldExpiryScheduler implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(HoldExpiryScheduler.class.getName());
    private static final long SHUTDOWN_WAIT_SECONDS = 5;

    private final BookingService bookingService;
    private final Duration interval;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong runs = new AtomicLong();
    private final AtomicLong totalExpired = new AtomicLong();

    public HoldExpiryScheduler(BookingService bookingService, Duration interval) {
        this.bookingService = Guard.notNull(bookingService, "bookingService");
        Guard.notNull(interval, "interval");
        Guard.that(!interval.isNegative() && !interval.isZero(), "interval must be positive");
        this.interval = interval;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "seatwise-hold-expiry");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            long millis = interval.toMillis();
            executor.scheduleAtFixedRate(this::runOnce, millis, millis, TimeUnit.MILLISECONDS);
            LOG.info(() -> "Hold-expiry scheduler started, every " + interval);
        }
    }

    public int runOnce() {
        runs.incrementAndGet();
        try {
            int expired = bookingService.expireHolds();
            totalExpired.addAndGet(expired);
            return expired;
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Hold-expiry sweep failed; will retry", e);
            return 0;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public long runs() {
        return runs.get();
    }

    public long totalExpired() {
        return totalExpired.get();
    }

    @Override
    public void close() {
        running.set(false);
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
