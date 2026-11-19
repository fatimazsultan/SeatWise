package com.seatwise.checkout;

import com.seatwise.booking.Booking;
import com.seatwise.booking.BookingService;
import com.seatwise.booking.Customer;
import com.seatwise.booking.event.BookingEvent;
import com.seatwise.booking.event.BookingEventPublisher;
import com.seatwise.booking.event.BookingEventType;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventService;
import com.seatwise.catalog.PriceCalculator;
import com.seatwise.catalog.Seat;
import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.Guard;
import com.seatwise.common.Money;
import com.seatwise.common.PaymentFailedException;
import com.seatwise.payment.Payment;
import com.seatwise.payment.PaymentMethod;
import com.seatwise.payment.PaymentService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Customer purchase flow: quote, hold, pay, confirm. If payment fails the hold is
 * cancelled and the seats go back on sale. This is the only class that talks to
 * catalog, booking and payment together.
 */
public class CheckoutService {

    private static final Logger LOG = Logger.getLogger(CheckoutService.class.getName());

    private final EventService events;
    private final BookingService bookings;
    private final PaymentService payments;
    private final BookingEventPublisher publisher;
    private final Clock clock;

    public CheckoutService(EventService events, BookingService bookings, PaymentService payments,
            BookingEventPublisher publisher, Clock clock) {
        this.events = Guard.notNull(events, "events");
        this.bookings = Guard.notNull(bookings, "bookings");
        this.payments = Guard.notNull(payments, "payments");
        this.publisher = Guard.notNull(publisher, "publisher");
        this.clock = Guard.notNull(clock, "clock");
    }

    public Money quote(String eventId, List<String> seatIds) {
        Event event = events.getById(eventId);
        return PriceCalculator.quote(event, resolveSeats(event, seatIds));
    }

    public Booking hold(String eventId, Customer customer, List<String> seatIds) {
        return bookings.hold(eventId, customer, seatIds, quote(eventId, seatIds));
    }

    public Booking payAndConfirm(String bookingId, PaymentMethod method, String instrumentRef) {
        Guard.notNull(method, "method");
        Guard.notBlank(instrumentRef, "instrumentRef");
        Booking booking = bookings.getById(bookingId);
        try {
            Payment payment = payments.charge(booking, booking.totalAmount(), method, instrumentRef);
            return bookings.confirm(bookingId, payment.id());
        } catch (PaymentFailedException e) {
            publisher.publish(new BookingEvent(BookingEventType.PAYMENT_FAILED, booking,
                    e.getMessage(), clock.instant()));
            bookings.cancel(bookingId, "Payment failed");
            LOG.warning(() -> "Rolled back hold " + bookingId + " after payment failure");
            throw e;
        }
    }

    public Booking checkout(String eventId, Customer customer, List<String> seatIds,
            PaymentMethod method, String instrumentRef) {
        Booking held = hold(eventId, customer, seatIds);
        return payAndConfirm(held.id(), method, instrumentRef);
    }

    private List<Seat> resolveSeats(Event event, List<String> seatIds) {
        Guard.notEmpty(seatIds, "seatIds");
        List<Seat> ofEvent = events.seatsOf(event.id());
        List<Seat> resolved = new ArrayList<>();
        for (String id : seatIds) {
            resolved.add(findSeat(ofEvent, id));
        }
        return resolved;
    }

    private static Seat findSeat(List<Seat> ofEvent, String id) {
        for (Seat seat : ofEvent) {
            if (seat.id().equals(id)) {
                return seat;
            }
        }
        throw new EntityNotFoundException("Seat", id);
    }
}
