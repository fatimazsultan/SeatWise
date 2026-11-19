package com.seatwise.booking.event;

/** Lifecycle events observers can react to. */
public enum BookingEventType {
    HELD,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    PAYMENT_FAILED
}
