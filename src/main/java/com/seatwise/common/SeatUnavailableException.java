package com.seatwise.common;

import java.util.Collection;
import java.util.List;

/** Thrown when one or more requested seats are already held or booked. No seat is changed. */
public class SeatUnavailableException extends SeatWiseException {

    private static final long serialVersionUID = 1L;
    public static final String CODE = "SEAT_UNAVAILABLE";

    private final List<String> unavailableSeatIds;

    public SeatUnavailableException(Collection<String> unavailableSeatIds) {
        super(CODE, "Seat(s) not available: " + String.join(", ", unavailableSeatIds));
        this.unavailableSeatIds = List.copyOf(unavailableSeatIds);
    }

    public List<String> unavailableSeatIds() {
        return unavailableSeatIds;
    }
}
