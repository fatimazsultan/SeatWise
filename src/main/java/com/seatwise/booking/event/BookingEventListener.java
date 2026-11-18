package com.seatwise.booking.event;

/** Observer. Implementations must tolerate being called from any thread. */
@FunctionalInterface
public interface BookingEventListener {

    void onBookingEvent(BookingEvent event);
}
