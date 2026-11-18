package com.seatwise.booking;

import com.seatwise.catalog.SeatStatus;
import java.util.List;
import java.util.Optional;

/** Persistence port for bookings. Seat status is updated in the same call so both stay consistent. */
public interface BookingRepository {

    void saveHold(Booking booking);

    void update(Booking booking, SeatStatus seatStatus);

    Optional<Booking> findById(String bookingId);

    List<Booking> findByEvent(String eventId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByCustomerEmail(String email);

    List<Booking> findAll();
}
