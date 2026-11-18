package com.seatwise.booking.event;

/** Subject of the Observer pattern. Booking publishes; notifications subscribe. */
public interface BookingEventPublisher {

    void subscribe(BookingEventListener listener);

    void unsubscribe(BookingEventListener listener);

    void publish(BookingEvent event);
}
