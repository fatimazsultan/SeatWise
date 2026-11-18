package com.seatwise.booking;

import com.seatwise.booking.event.BookingEvent;
import com.seatwise.booking.event.BookingEventPublisher;
import com.seatwise.booking.event.BookingEventType;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventRepository;
import com.seatwise.catalog.SeatRepository;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.Guard;
import com.seatwise.common.Ids;
import com.seatwise.common.Money;
import com.seatwise.common.PersistenceException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds, confirms, cancels and expires bookings.
 *
 * <p>Thread safety: seat changes take the per-event lock inside {@link SeatInventory}.
 * Lifecycle changes for one booking take a per-booking lock here. Lock order is always
 * booking-lock then inventory-lock, and inventory never calls back, so there is no deadlock.
 * If the database write fails, the inventory change is rolled back.</p>
 */
public class BookingService {

    private static final Logger LOG = Logger.getLogger(BookingService.class.getName());

    private final BookingRepository bookings;
    private final EventRepository events;
    private final SeatRepository seats;
    private final SeatInventory inventory;
    private final BookingEventPublisher publisher;
    private final Clock clock;
    private final Duration holdDuration;
    private final ConcurrentHashMap<String, ReentrantLock> bookingLocks = new ConcurrentHashMap<>();

    public BookingService(BookingRepository bookings, EventRepository events, SeatRepository seats,
            SeatInventory inventory, BookingEventPublisher publisher, Clock clock, Duration holdDuration) {
        this.bookings = Guard.notNull(bookings, "bookings");
        this.events = Guard.notNull(events, "events");
        this.seats = Guard.notNull(seats, "seats");
        this.inventory = Guard.notNull(inventory, "inventory");
        this.publisher = Guard.notNull(publisher, "publisher");
        this.clock = Guard.notNull(clock, "clock");
        Guard.notNull(holdDuration, "holdDuration");
        Guard.that(!holdDuration.isNegative() && !holdDuration.isZero(), "holdDuration must be positive");
        this.holdDuration = holdDuration;
    }

    public void loadInventory(String eventId) {
        Guard.notBlank(eventId, "eventId");
        inventory.load(eventId, seats.findStatusByEvent(eventId));
    }

    public Booking hold(String eventId, Customer customer, List<String> seatIds, Money quotedTotal) {
        Guard.notBlank(eventId, "eventId");
        Guard.notNull(customer, "customer");
        Guard.notEmpty(seatIds, "seatIds");
        Instant now = clock.instant();
        Event event = requireEvent(eventId);
        Guard.that(event.isOnSale(now), "Event " + eventId + " is not on sale");
        if (!inventory.hasEvent(eventId)) {
            loadInventory(eventId);
        }
        Booking booking = new Booking(Ids.next("BK"), eventId, customer, seatIds, quotedTotal, now);
        inventory.hold(eventId, seatIds);
        try {
            booking.markHeld(now.plus(holdDuration), now);
            bookings.saveHold(booking);
        } catch (PersistenceException e) {
            inventory.release(eventId, seatIds);
            LOG.log(Level.SEVERE, "Persistence failed while holding seats; inventory compensated", e);
            throw e;
        }
        publisher.publish(BookingEvent.of(BookingEventType.HELD, booking, now));
        return booking;
    }

    public Booking confirm(String bookingId, String paymentId) {
        Guard.notBlank(paymentId, "paymentId");
        return withBookingLock(bookingId, () -> confirmLocked(bookingId, paymentId));
    }

    public Booking cancel(String bookingId, String reason) {
        return withBookingLock(bookingId, () -> cancelLocked(bookingId, reason));
    }

    public int expireHolds() {
        Instant now = clock.instant();
        int expired = 0;
        for (Booking candidate : bookings.findByStatus(BookingStatus.HELD)) {
            if (candidate.isHoldExpired(now) && expireOne(candidate.id(), now)) {
                expired++;
            }
        }
        return expired;
    }

    public Booking getById(String bookingId) {
        return requireBooking(bookingId);
    }

    public List<Booking> findByEvent(String eventId) {
        Guard.notBlank(eventId, "eventId");
        return bookings.findByEvent(eventId);
    }

    public List<Booking> findByCustomerEmail(String email) {
        return bookings.findByCustomerEmail(Guard.email(email, "email"));
    }

    public List<Booking> findAll() {
        return bookings.findAll();
    }

    public SeatInventory inventory() {
        return inventory;
    }

    public Duration holdDuration() {
        return holdDuration;
    }

    private Booking confirmLocked(String bookingId, String paymentId) {
        Booking booking = requireBooking(bookingId);
        Instant now = clock.instant();
        Guard.that(!booking.isHoldExpired(now), "Hold for booking " + bookingId + " has expired");
        booking.confirm(paymentId, now);
        inventory.confirm(booking.eventId(), booking.seatIds());
        try {
            bookings.update(booking, SeatStatus.BOOKED);
        } catch (PersistenceException e) {
            inventory.undoConfirm(booking.eventId(), booking.seatIds());
            LOG.log(Level.SEVERE, "Persistence failed while confirming; inventory compensated", e);
            throw e;
        }
        publisher.publish(BookingEvent.of(BookingEventType.CONFIRMED, booking, now));
        return booking;
    }

    private Booking cancelLocked(String bookingId, String reason) {
        Booking booking = requireBooking(bookingId);
        BookingStatus before = booking.status();
        Instant now = clock.instant();
        booking.cancel(now);
        releaseInventory(booking, before);
        bookings.update(booking, SeatStatus.AVAILABLE);
        publisher.publish(new BookingEvent(BookingEventType.CANCELLED, booking, reason, now));
        return booking;
    }

    private boolean expireOne(String bookingId, Instant now) {
        return withBookingLock(bookingId, () -> {
            Booking booking = requireBooking(bookingId);
            if (!booking.isHoldExpired(now)) {
                return false;
            }
            booking.expire(now);
            inventory.release(booking.eventId(), booking.seatIds());
            bookings.update(booking, SeatStatus.AVAILABLE);
            publisher.publish(BookingEvent.of(BookingEventType.EXPIRED, booking, now));
            return true;
        });
    }

    private void releaseInventory(Booking booking, BookingStatus previous) {
        if (previous == BookingStatus.HELD) {
            inventory.release(booking.eventId(), booking.seatIds());
        } else if (previous == BookingStatus.CONFIRMED) {
            inventory.releaseBooked(booking.eventId(), booking.seatIds());
        }
    }

    private Event requireEvent(String eventId) {
        return events.findById(eventId).orElseThrow(() -> new EntityNotFoundException("Event", eventId));
    }

    private Booking requireBooking(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return bookings.findById(bookingId).orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));
    }

    private <T> T withBookingLock(String bookingId, Supplier<T> action) {
        Guard.notBlank(bookingId, "bookingId");
        ReentrantLock lock = bookingLocks.computeIfAbsent(bookingId, key -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
