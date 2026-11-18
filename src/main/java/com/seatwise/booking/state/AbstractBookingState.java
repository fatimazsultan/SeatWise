package com.seatwise.booking.state;

import com.seatwise.common.InvalidStateTransitionException;

/** Rejects every transition. Concrete states override only the legal ones. */
abstract class AbstractBookingState implements BookingState {

    @Override
    public BookingState hold() {
        throw illegal("hold");
    }

    @Override
    public BookingState confirm() {
        throw illegal("confirm");
    }

    @Override
    public BookingState cancel() {
        throw illegal("cancel");
    }

    @Override
    public BookingState expire() {
        throw illegal("expire");
    }

    protected final InvalidStateTransitionException illegal(String operation) {
        return new InvalidStateTransitionException("booking", status().name(), operation);
    }
}
