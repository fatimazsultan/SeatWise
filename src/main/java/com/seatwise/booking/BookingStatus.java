package com.seatwise.booking;

/** Persisted lifecycle of a booking. Legal moves are enforced by the State classes. */
public enum BookingStatus {
    PENDING,
    HELD,
    CONFIRMED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }
}
