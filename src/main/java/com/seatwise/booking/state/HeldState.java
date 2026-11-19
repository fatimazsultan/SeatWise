package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;

/** Seats are reserved until the hold deadline. */
final class HeldState extends AbstractBookingState {

    @Override
    public BookingStatus status() {
        return BookingStatus.HELD;
    }

    @Override
    public BookingState confirm() {
        return BookingStates.CONFIRMED;
    }

    @Override
    public BookingState cancel() {
        return BookingStates.CANCELLED;
    }

    @Override
    public BookingState expire() {
        return BookingStates.EXPIRED;
    }
}
