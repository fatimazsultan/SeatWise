package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;

/** Terminal state reached when a hold is not paid in time. */
final class ExpiredState extends AbstractBookingState {

    @Override
    public BookingStatus status() {
        return BookingStatus.EXPIRED;
    }
}
