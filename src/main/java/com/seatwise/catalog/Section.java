package com.seatwise.catalog;

import com.seatwise.common.Guard;
import com.seatwise.common.Money;

/** One priced block of seats. Rows are labelled A, B, C up to Z. */
public record Section(String name, SeatTier tier, int rows, int seatsPerRow, Money basePrice) {

    private static final int MAX_ROWS = 26;
    private static final int MAX_SEATS_PER_ROW = 50;

    public Section {
        Guard.notBlank(name, "section.name");
        Guard.notNull(tier, "section.tier");
        Guard.inRange(rows, 1, MAX_ROWS, "section.rows");
        Guard.inRange(seatsPerRow, 1, MAX_SEATS_PER_ROW, "section.seatsPerRow");
        Guard.notNegative(basePrice, "section.basePrice");
        Guard.that(!basePrice.isZero(), "section.basePrice must be greater than zero");
    }

    public int capacity() {
        return rows * seatsPerRow;
    }
}
