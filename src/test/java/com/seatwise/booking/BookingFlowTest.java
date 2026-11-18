package com.seatwise.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventStatus;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.InvalidStateTransitionException;
import com.seatwise.common.Money;
import com.seatwise.common.PaymentFailedException;
import com.seatwise.common.SeatUnavailableException;
import com.seatwise.common.ValidationException;
import com.seatwise.notification.NotificationChannel;
import com.seatwise.payment.DecliningPaymentGateway;
import com.seatwise.payment.PaymentMethod;
import com.seatwise.payment.PaymentStatus;
import com.seatwise.support.Fixtures;
import com.seatwise.support.InMemoryHarness;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BookingFlowTest {

    @Test
    void holdPayConfirmThenCancelReleasesSeats() {
        InMemoryHarness app = InMemoryHarness.create();
        Event event = app.publishedConcert();
        Customer customer = new Customer("Aisha Khan", "aisha@example.com", "03001112233");
        List<String> seats = List.of(Fixtures.seat("VIP", "A", 1), Fixtures.seat("VIP", "A", 2));
        assertEquals(Money.of("2000.00"), app.checkout.quote(event.id(), seats));

        Booking held = app.checkout.hold(event.id(), customer, seats);
        assertEquals(BookingStatus.HELD, held.status());
        assertEquals(SeatStatus.HELD, app.inventory.statusOf(event.id(), seats.get(0)));

        Booking paid = app.checkout.payAndConfirm(held.id(), PaymentMethod.CARD, "4242424242424242");
        assertEquals(BookingStatus.CONFIRMED, paid.status());
        assertEquals(SeatStatus.BOOKED, app.inventory.statusOf(event.id(), seats.get(0)));
        assertEquals(PaymentStatus.SUCCESS, app.payments.historyOf(held.id()).get(0).status());
        assertTrue(app.notifications.forBooking(held.id()).stream()
                .anyMatch(note -> note.channel() == NotificationChannel.SMS));

        app.bookings.cancel(paid.id(), "changed plans");
        assertEquals(BookingStatus.CANCELLED, app.bookings.getById(paid.id()).status());
        assertEquals(SeatStatus.AVAILABLE, app.inventory.statusOf(event.id(), seats.get(0)));
    }

    @Test
    void declinedPaymentReleasesTheHold() {
        InMemoryHarness app = InMemoryHarness.create();
        Event event = app.publishedConcert();
        String seat = Fixtures.seat("Std", "A", 1);
        Booking held = app.checkout.hold(event.id(), new Customer("Bilal", "bilal@example.com"), List.of(seat));
        assertThrows(PaymentFailedException.class,
                () -> app.checkout.payAndConfirm(held.id(), PaymentMethod.WALLET, "FAIL-0000"));
        assertEquals(BookingStatus.CANCELLED, app.bookings.getById(held.id()).status());
        assertEquals(SeatStatus.AVAILABLE, app.inventory.statusOf(event.id(), seat));
        assertEquals(1, app.payments.historyOf(held.id()).size());
    }

    @Test
    void gatewayOutageIsRecordedAndHoldIsRolledBack() {
        InMemoryHarness app = InMemoryHarness.create(new DecliningPaymentGateway("bank down"));
        Event event = app.publishedConcert();
        String seat = Fixtures.seat("Std", "B", 1);
        Booking held = app.checkout.hold(event.id(), new Customer("Sara", "sara@example.com"), List.of(seat));
        PaymentFailedException error = assertThrows(PaymentFailedException.class,
                () -> app.checkout.payAndConfirm(held.id(), PaymentMethod.BANK_TRANSFER, "acct-9"));
        assertTrue(error.getMessage().contains("bank down"));
        assertEquals(SeatStatus.AVAILABLE, app.inventory.statusOf(event.id(), seat));
    }

    @Test
    void secondCustomerCannotTakeAHeldSeatAndExpiredHoldFreesIt() {
        InMemoryHarness app = InMemoryHarness.create();
        Event event = app.publishedConcert();
        String seat = Fixtures.seat("VIP", "B", 1);
        Booking held = app.checkout.hold(event.id(), new Customer("Aisha", "aisha@example.com"), List.of(seat));
        assertThrows(SeatUnavailableException.class,
                () -> app.checkout.hold(event.id(), new Customer("Omar", "omar@example.com"), List.of(seat)));
        app.clock.advance(Duration.ofMinutes(10));
        assertEquals(1, app.bookings.expireHolds());
        assertEquals(BookingStatus.EXPIRED, app.bookings.getById(held.id()).status());
        assertEquals(SeatStatus.AVAILABLE, app.inventory.statusOf(event.id(), seat));
        Booking again = app.checkout.hold(event.id(), new Customer("Omar", "omar@example.com"), List.of(seat));
        assertEquals(BookingStatus.HELD, again.status());
    }

    @Test
    void unpublishedEventAndUnknownIdsAreRejected() {
        InMemoryHarness app = InMemoryHarness.create();
        Event draft = Fixtures.unpublishedConcert(app.clock);
        app.events.create(draft);
        assertThrows(ValidationException.class, () -> app.checkout.hold(draft.id(),
                new Customer("Aisha", "aisha@example.com"), List.of(Fixtures.seat("VIP", "A", 1))));
        assertThrows(EntityNotFoundException.class, () -> app.events.getById("missing"));
        assertThrows(EntityNotFoundException.class, () -> app.bookings.getById("missing"));
        app.events.publish(draft.id());
        assertThrows(InvalidStateTransitionException.class, () -> app.events.publish(draft.id()));
        app.events.cancel(draft.id());
        assertEquals(EventStatus.CANCELLED, app.events.getById(draft.id()).status());
    }

    @Test
    void eightThreadsRacingForOneSeatProduceExactlyOneWinner() throws Exception {
        InMemoryHarness app = InMemoryHarness.create();
        Event event = app.publishedConcert();
        String seat = Fixtures.seat("Std", "A", 4);
        int racers = 8;
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger losses = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(racers);
        ExecutorService pool = Executors.newFixedThreadPool(racers);
        for (int i = 0; i < racers; i++) {
            int n = i;
            pool.execute(() -> {
                await(start);
                try {
                    app.checkout.hold(event.id(), new Customer("Racer " + n, "r" + n + "@example.com"), List.of(seat));
                    wins.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    losses.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(1, wins.get());
        assertEquals(racers - 1, losses.get());
        assertTrue(app.inventory.holdConflicts() >= racers - 1);
    }

    @Test
    void schedulerExpiresAHoldWithoutThrowing() {
        InMemoryHarness app = InMemoryHarness.create();
        Event event = app.publishedConcert();
        app.checkout.hold(event.id(), new Customer("Aisha", "aisha@example.com"),
                List.of(Fixtures.seat("VIP", "A", 3)));
        app.clock.advance(app.bookings.holdDuration());
        try (HoldExpiryScheduler scheduler = new HoldExpiryScheduler(app.bookings, Duration.ofMillis(50))) {
            assertEquals(1, scheduler.runOnce());
            assertEquals(1, scheduler.runs());
            assertEquals(1, scheduler.totalExpired());
            scheduler.start();
            assertTrue(scheduler.isRunning());
            assertEquals(0, scheduler.runOnce());
        }
    }

    @Test
    void quotedAmountMustMatchAndTooManySeatsFail() {
        InMemoryHarness app = InMemoryHarness.create();
        Event event = app.publishedConcert();
        List<String> tooMany = new ArrayList<>();
        for (int n = 1; n <= 4; n++) {
            tooMany.add(Fixtures.seat("VIP", "A", n));
            tooMany.add(Fixtures.seat("VIP", "B", n));
            tooMany.add(Fixtures.seat("Std", "A", n));
        }
        assertThrows(ValidationException.class,
                () -> app.checkout.hold(event.id(), new Customer("Aisha", "aisha@example.com"), tooMany));
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
