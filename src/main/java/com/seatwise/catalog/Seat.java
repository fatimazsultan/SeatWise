package com.seatwise.catalog;

import com.seatwise.common.Guard;

/**
 * Immutable catalogue description of a seat. Live availability lives in
 * {@link com.seatwise.booking.SeatInventory}, not on this object.
 */
public record Seat(String id, String eventId, String section, String row, int number, SeatTier tier) {

    public Seat {
        Guard.notBlank(id, "seat.id");
        Guard.notBlank(eventId, "seat.eventId");
        Guard.notBlank(section, "seat.section");
        Guard.notBlank(row, "seat.row");
        Guard.positive(number, "seat.number");
        Guard.notNull(tier, "seat.tier");
    }

    public static String idOf(String eventId, String section, String row, int number) {
        return eventId + ":" + section + "-" + row + "-" + number;
    }

    public String label() {
        return section + " " + row + "-" + number;
    }
}
