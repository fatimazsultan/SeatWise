package com.seatwise.booking.event;

import com.seatwise.booking.Booking;
import com.seatwise.common.Guard;
import java.time.Instant;

/** Immutable notice that something happened to a booking. */
public record BookingEvent(BookingEventType type, Booking booking, String detail, Instant occurredAt) {

    public BookingEvent {
        Guard.notNull(type, "type");
        Guard.notNull(booking, "booking");
        Guard.notNull(occurredAt, "occurredAt");
        detail = detail == null ? "" : detail;
    }

    public static BookingEvent of(BookingEventType type, Booking booking, Instant now) {
        return new BookingEvent(type, booking, "", now);
    }
}
