package com.seatwise.persistence.memory;

import com.seatwise.booking.Booking;
import com.seatwise.booking.BookingRepository;
import com.seatwise.booking.BookingStatus;
import com.seatwise.catalog.SeatRepository;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.Guard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory bookings. Seat status is updated through the seat repository. */
public final class InMemoryBookingRepository implements BookingRepository {

    private final ConcurrentHashMap<String, Booking> store = new ConcurrentHashMap<>();
    private final SeatRepository seats;

    public InMemoryBookingRepository(SeatRepository seats) {
        this.seats = Guard.notNull(seats, "seats");
    }

    @Override
    public void saveHold(Booking booking) {
        store.put(Guard.notNull(booking, "booking").id(), booking);
        seats.updateStatus(booking.seatIds(), SeatStatus.HELD);
    }

    @Override
    public void update(Booking booking, SeatStatus seatStatus) {
        store.put(Guard.notNull(booking, "booking").id(), booking);
        seats.updateStatus(booking.seatIds(), Guard.notNull(seatStatus, "seatStatus"));
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    @Override
    public List<Booking> findByEvent(String eventId) {
        Guard.notBlank(eventId, "eventId");
        return store.values().stream().filter(booking -> eventId.equals(booking.eventId())).toList();
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        Guard.notNull(status, "status");
        return store.values().stream().filter(booking -> booking.status() == status).toList();
    }

    @Override
    public List<Booking> findByCustomerEmail(String email) {
        Guard.notBlank(email, "email");
        return store.values().stream()
                .filter(booking -> email.equalsIgnoreCase(booking.customer().email()))
                .toList();
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(store.values());
    }
}
