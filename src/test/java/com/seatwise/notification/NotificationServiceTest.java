package com.seatwise.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seatwise.booking.Booking;
import com.seatwise.booking.Customer;
import com.seatwise.booking.event.BookingEvent;
import com.seatwise.booking.event.BookingEventType;
import com.seatwise.common.PaymentFailedException;
import com.seatwise.common.ValidationException;
import com.seatwise.payment.PaymentMethod;
import com.seatwise.support.Fixtures;
import com.seatwise.support.InMemoryHarness;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Observer behaviour of the notifications module (owner: Abdul). */
class NotificationServiceTest {

    private static List<String> channels(List<Notification> notes) {
        return notes.stream().map(note -> note.channel().name()).sorted().toList();
    }

    @Test
    void holdWithoutPhoneSendsEmailAndInAppOnly() {
        InMemoryHarness app = InMemoryHarness.create();
        Booking held = app.checkout.hold(app.publishedConcert().id(), new Customer("Aisha", "aisha@example.com"),
                List.of(Fixtures.seat("Std", "A", 1)));
        List<Notification> notes = app.notifications.forBooking(held.id());
        assertEquals(List.of("EMAIL", "IN_APP"), channels(notes));
        assertEquals("Seats held", notes.get(0).subject());
    }

    @Test
    void customerWithPhoneAlsoGetsSms() {
        InMemoryHarness app = InMemoryHarness.create();
        Booking held = app.checkout.hold(app.publishedConcert().id(),
                new Customer("Bilal", "bilal@example.com", "+92 300 1112233"),
                List.of(Fixtures.seat("Std", "A", 2)));
        assertEquals(List.of("EMAIL", "IN_APP", "SMS"), channels(app.notifications.forBooking(held.id())));
    }

    @Test
    void confirmedAndFailedPaymentProduceTheirOwnMessages() {
        InMemoryHarness app = InMemoryHarness.create();
        String eventId = app.publishedConcert().id();
        Booking ok = app.checkout.hold(eventId, new Customer("Sara", "sara@example.com"),
                List.of(Fixtures.seat("Std", "B", 1)));
        app.checkout.payAndConfirm(ok.id(), PaymentMethod.CARD, "4242424242424242");
        assertTrue(app.notifications.forBooking(ok.id()).stream()
                .anyMatch(note -> note.subject().equals("Booking confirmed")));

        Booking bad = app.checkout.hold(eventId, new Customer("Omar", "omar@example.com"),
                List.of(Fixtures.seat("Std", "B", 2)));
        assertThrows(PaymentFailedException.class,
                () -> app.checkout.payAndConfirm(bad.id(), PaymentMethod.CARD, "FAIL-card"));
        assertTrue(app.notifications.forBooking(bad.id()).stream()
                .anyMatch(note -> note.subject().equals("Payment failed")));
    }

    @Test
    void everyEventTypeHasAMessageAndAllIsAggregated() {
        InMemoryHarness app = InMemoryHarness.create();
        Booking held = app.checkout.hold(app.publishedConcert().id(), new Customer("Zoya", "zoya@example.com"),
                List.of(Fixtures.seat("Std", "C", 1)));
        int before = app.notifications.all().size();
        for (BookingEventType type : BookingEventType.values()) {
            app.notifications.onBookingEvent(BookingEvent.of(type, held, app.clock.instant()));
        }
        assertEquals(before + 2 * BookingEventType.values().length, app.notifications.all().size());
    }

    @Test
    void nullEventAndBlankBookingIdAreRejected() {
        InMemoryHarness app = InMemoryHarness.create();
        assertThrows(ValidationException.class, () -> app.notifications.onBookingEvent(null));
        assertThrows(ValidationException.class, () -> app.notifications.forBooking(" "));
    }
}
