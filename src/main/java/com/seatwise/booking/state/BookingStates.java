package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;
import com.seatwise.common.Guard;

/** Shared stateless state objects, reused like flyweights. */
public final class BookingStates {

    public static final BookingState PENDING = new PendingState();
    public static final BookingState HELD = new HeldState();
    public static final BookingState CONFIRMED = new ConfirmedState();
    public static final BookingState CANCELLED = new CancelledState();
    public static final BookingState EXPIRED = new ExpiredState();

    private BookingStates() {
    }

    public static BookingState of(BookingStatus status) {
        Guard.notNull(status, "status");
        return switch (status) {
            case PENDING -> PENDING;
            case HELD -> HELD;
            case CONFIRMED -> CONFIRMED;
            case CANCELLED -> CANCELLED;
            case EXPIRED -> EXPIRED;
        };
    }
}
