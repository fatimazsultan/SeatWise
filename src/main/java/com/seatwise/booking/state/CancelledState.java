package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;

/** Terminal state. */
final class CancelledState extends AbstractBookingState {

    @Override
    public BookingStatus status() {
        return BookingStatus.CANCELLED;
    }
}
