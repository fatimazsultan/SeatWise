package com.seatwise.catalog;

import com.seatwise.common.Guard;
import com.seatwise.common.Money;
import java.util.List;

/** Quotes a booking total from section base prices. */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static Money quote(Event event, List<Seat> seats) {
        Guard.notNull(event, "event");
        Guard.notEmpty(seats, "seats");
        Money total = Money.ZERO;
        for (Seat seat : seats) {
            Guard.that(event.id().equals(seat.eventId()), "Seat " + seat.id() + " is not part of " + event.id());
            total = total.plus(event.basePriceOf(seat.section()));
        }
        return total;
    }
}
