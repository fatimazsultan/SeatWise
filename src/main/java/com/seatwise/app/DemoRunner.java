package com.seatwise.app;

import com.seatwise.booking.Booking;
import com.seatwise.booking.Customer;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.Seat;
import com.seatwise.common.MutableClock;
import com.seatwise.common.PaymentFailedException;
import com.seatwise.common.SeatUnavailableException;
import com.seatwise.payment.PaymentMethod;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-interactive walkthrough used by CI ({@code --demo}) and the instructor demo.
 * Covers a paid booking, a declined card, hold expiry, and a concurrent seat race.
 */
public final class DemoRunner {

    private static final Duration HOLD = Duration.ofMinutes(10);
    private static final int RACERS = 8;
    private static final long DRAIN_MS = 400;
    private static final long RACE_WAIT_SECONDS = 10;

    private DemoRunner() {
    }

    public static void run(Path databaseFile) {
        MutableClock clock = MutableClock.epoch();
        try (SeatWiseApp app = SeatWiseApp.open(AppConfig.demo(databaseFile, clock, HOLD))) {
            runScenarios(app, clock);
        }
    }

    static void runScenarios(SeatWiseApp app, MutableClock clock) {
        Event event = app.events().listUpcoming().get(0);
        System.out.println("=== SeatWise demo ===");
        System.out.println("Event: " + event);
        System.out.println(SeatMapFormatter.format(app.events().seatsOf(event.id()),
                app.inventory().snapshot(event.id())));
        Booking paid = happyPath(app, event);
        System.out.println("CONFIRMED " + paid);
        paymentFailure(app, event);
        holdExpiry(app, event, clock);
        concurrentRace(app, event);
        pause(DRAIN_MS);
        System.out.println("Notifications: " + app.notifications().all().size());
        app.notifications().all().forEach(note -> System.out.println("  " + note));
        System.out.println("Inventory " + app.inventory().counts(event.id()));
        System.out.println("Hold attempts=" + app.inventory().holdAttempts()
                + " conflicts=" + app.inventory().holdConflicts());
        System.out.println("=== demo complete ===");
    }

    private static Booking happyPath(SeatWiseApp app, Event event) {
        Customer customer = new Customer("Aisha Khan", "aisha@example.com", "+923001112233");
        List<String> seats = List.of(Seat.idOf(event.id(), "VIP", "A", 1), Seat.idOf(event.id(), "VIP", "A", 2));
        Booking held = app.checkout().hold(event.id(), customer, seats);
        System.out.println("HELD " + held.id() + " total " + held.totalAmount());
        return app.checkout().payAndConfirm(held.id(), PaymentMethod.CARD, "4242424242424242");
    }

    private static void paymentFailure(SeatWiseApp app, Event event) {
        Customer customer = new Customer("Bilal Ahmed", "bilal@example.com");
        String seatId = Seat.idOf(event.id(), "Standard", "A", 1);
        Booking held = app.checkout().hold(event.id(), customer, List.of(seatId));
        try {
            app.checkout().payAndConfirm(held.id(), PaymentMethod.CARD, "FAIL-0000");
            throw new IllegalStateException("expected payment failure");
        } catch (PaymentFailedException e) {
            System.out.println("PAYMENT FAILED as expected: " + e.getMessage());
            System.out.println("  seat now " + app.inventory().statusOf(event.id(), seatId));
        }
    }

    private static void holdExpiry(SeatWiseApp app, Event event, MutableClock clock) {
        Customer customer = new Customer("Sara Malik", "sara@example.com");
        Booking held = app.checkout().hold(event.id(), customer,
                List.of(Seat.idOf(event.id(), "Premium", "A", 1)));
        clock.advance(HOLD.plusSeconds(1));
        int expired = app.bookings().expireHolds();
        System.out.println("EXPIRED " + expired + " hold(s); " + held.id() + " is "
                + app.bookings().getById(held.id()).status());
    }

    private static void concurrentRace(SeatWiseApp app, Event event) {
        String contested = Seat.idOf(event.id(), "Premium", "B", 1);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger losses = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(RACERS);
        ExecutorService pool = Executors.newFixedThreadPool(RACERS);
        for (int i = 0; i < RACERS; i++) {
            int racer = i;
            pool.execute(() -> raceOnce(app, event, contested, racer, wins, losses, start, done));
        }
        start.countDown();
        await(done);
        pool.shutdownNow();
        System.out.println("RACE winners=" + wins.get() + " losers=" + losses.get());
        if (wins.get() != 1) {
            throw new IllegalStateException("expected exactly one winner, got " + wins.get());
        }
    }

    private static void raceOnce(SeatWiseApp app, Event event, String contested, int racer, AtomicInteger wins,
            AtomicInteger losses, CountDownLatch start, CountDownLatch done) {
        await(start);
        Customer customer = new Customer("Racer " + racer, "racer" + racer + "@example.com");
        try {
            app.checkout().hold(event.id(), customer, List.of(contested));
            wins.incrementAndGet();
        } catch (SeatUnavailableException e) {
            losses.incrementAndGet();
        } finally {
            done.countDown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(RACE_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for demo workers");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
