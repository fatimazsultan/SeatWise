package com.seatwise.support;

import com.seatwise.catalog.Event;
import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatTier;
import com.seatwise.catalog.Section;
import com.seatwise.catalog.Venue;
import com.seatwise.common.Money;
import com.seatwise.common.MutableClock;
import java.time.Duration;

/** Shared event fixture for tests. */
public final class Fixtures {

    private Fixtures() {
    }

    public static MutableClock clock() {
        return MutableClock.epoch();
    }

    public static Event unpublishedConcert(MutableClock clock) {
        return Event.builder()
                .id("EV-test")
                .title("Test Concert")
                .description("Fixture")
                .category("Music")
                .venue(new Venue("VN-1", "Test Hall", "Lahore"))
                .startsAt(clock.instant().plus(Duration.ofDays(10)))
                .salesOpenAt(clock.instant().minus(Duration.ofDays(1)))
                .addSection(new Section("VIP", SeatTier.VIP, 2, 4, Money.of("1000")))
                .addSection(new Section("Std", SeatTier.STANDARD, 2, 4, Money.of("500")))
                .build();
    }

    public static String seat(String section, String row, int number) {
        return Seat.idOf("EV-test", section, row, number);
    }
}
