package com.seatwise.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seatwise.booking.Booking;
import com.seatwise.booking.BookingStatus;
import com.seatwise.booking.Customer;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventStatus;
import com.seatwise.catalog.PriceCalculator;
import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatMapGenerator;
import com.seatwise.catalog.SeatTier;
import com.seatwise.catalog.Section;
import com.seatwise.support.Fixtures;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRulesTest {

    @Test
    void moneyRoundsAndRejectsGarbage() {
        assertEquals(Money.of("10.56"), Money.of("10.555"));
        assertEquals(Money.of("21.12"), Money.of("10.56").times(2));
        assertEquals(Money.of("1.56"), Money.of("10.40").percent(15));
        assertEquals(Money.of("5.00"), Money.of("10").minus(Money.of("5")));
        assertTrue(Money.of("-1").isNegative());
        assertThrows(ValidationException.class, () -> Money.of("nope"));
        assertThrows(ValidationException.class, () -> Money.of("   "));
    }

    @Test
    void customerAndGuardRejectBadInput() {
        Customer ok = new Customer("Aisha Khan", "aisha@example.com", "+92 300 1112233");
        assertEquals("aisha@example.com", ok.email());
        assertTrue(ok.phoneNumber().isPresent());
        assertThrows(ValidationException.class, () -> new Customer("A", "not-an-email"));
        assertThrows(ValidationException.class, () -> new Customer(" ", "a@b.co"));
        assertThrows(ValidationException.class, () -> new Customer("Aisha", "aisha@example.com", "123"));
        assertThrows(ValidationException.class, () -> Guard.notEmpty(List.of(), "items"));
        assertThrows(ValidationException.class, () -> Guard.positive(0, "count"));
    }

    @Test
    void eventBuilderValidatesAndSeatMapIsPriced() {
        MutableClock clock = Fixtures.clock();
        Event event = Fixtures.unpublishedConcert(clock);
        assertEquals(EventStatus.DRAFT, event.status());
        assertEquals(16, event.capacity());
        List<Seat> seats = SeatMapGenerator.generate(event);
        assertEquals(16, seats.size());
        assertEquals(SeatTier.VIP, seats.get(0).tier());
        assertEquals(Money.of("2000.00"), PriceCalculator.quote(event, List.of(seats.get(0), seats.get(1))));
        assertThrows(ValidationException.class, () -> Event.builder()
                .id("x").title("t").category("c")
                .venue(event.venue())
                .startsAt(clock.instant())
                .salesOpenAt(clock.instant().plus(Duration.ofDays(1)))
                .addSection(event.sections().get(0))
                .build());
        assertThrows(ValidationException.class, () -> new Section("A", SeatTier.VIP, 1, 1, Money.ZERO));
    }

    @Test
    void bookingStateMachineRejectsIllegalMoves() {
        MutableClock clock = Fixtures.clock();
        Customer customer = new Customer("Aisha", "aisha@example.com");
        Booking booking = new Booking("BK-1", "EV-test", customer, List.of("EV-test:VIP-A-1"),
                Money.of("1000"), clock.instant());
        assertEquals(BookingStatus.PENDING, booking.status());
        assertThrows(InvalidStateTransitionException.class, () -> booking.confirm("pay", clock.instant()));
        booking.markHeld(clock.instant().plus(Duration.ofMinutes(5)), clock.instant());
        assertEquals(BookingStatus.HELD, booking.status());
        booking.confirm("pay-1", clock.instant());
        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertThrows(InvalidStateTransitionException.class, () -> booking.expire(clock.instant()));
        booking.cancel(clock.instant());
        assertEquals(BookingStatus.CANCELLED, booking.status());
        assertTrue(booking.status().isTerminal());
        assertThrows(ValidationException.class, () -> new Booking("BK-2", "EV-test", customer,
                java.util.Collections.nCopies(11, "s"), Money.of("1"), clock.instant()));
    }
}
