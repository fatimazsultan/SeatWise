package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;

/** Paid booking. It can be cancelled, and it never expires. */
final class ConfirmedState extends AbstractBookingState {

    @Override
    public BookingStatus status() {
        return BookingStatus.CONFIRMED;
    }

    @Override
    public BookingState cancel() {
        return BookingStates.CANCELLED;
    }
}
