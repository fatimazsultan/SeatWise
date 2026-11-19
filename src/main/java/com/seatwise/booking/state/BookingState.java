package com.seatwise.booking.state;

import com.seatwise.booking.BookingStatus;

/**
 * State pattern. Each lifecycle state decides which transitions are legal, so adding a
 * state does not require editing a central switch.
 */
public interface BookingState {

    BookingStatus status();

    BookingState hold();

    BookingState confirm();

    BookingState cancel();

    BookingState expire();
}
