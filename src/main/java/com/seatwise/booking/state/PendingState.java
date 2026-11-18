package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;

/** Booking exists but seats are not reserved yet. */
final class PendingState extends AbstractBookingState {

    @Override
    public BookingStatus status() {
        return BookingStatus.PENDING;
    }

    @Override
    public BookingState hold() {
        return BookingStates.HELD;
    }

    @Override
    public BookingState cancel() {
        return BookingStates.CANCELLED;
    }
}
