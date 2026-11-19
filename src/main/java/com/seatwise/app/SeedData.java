package com.seatwise.app;

import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventService;
import com.seatwise.catalog.SeatTier;
import com.seatwise.catalog.Section;
import com.seatwise.catalog.Venue;
import com.seatwise.common.Ids;
import com.seatwise.common.Money;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/** Loads two published events so a fresh database can be used immediately. */
public final class SeedData {

    private static final Logger LOG = Logger.getLogger(SeedData.class.getName());
    private static final int VIP_ROWS = 2;
    private static final int VIP_SEATS = 6;
    private static final int PREMIUM_ROWS = 3;
    private static final int PREMIUM_SEATS = 8;
    private static final int STANDARD_ROWS = 4;
    private static final int STANDARD_SEATS = 10;
    private static final int PAVILION_SEATS = 8;
    private static final int ENCLOSURE_ROWS = 5;
    private static final int ENCLOSURE_SEATS = 12;

    private SeedData() {
    }

    public static void populate(EventService events, Clock clock) {
        Instant now = clock.instant();
        Instant salesOpen = now.minus(Duration.ofDays(30));
        Instant starts = now.plus(Duration.ofDays(60));
        Event concert = concert(salesOpen, starts);
        Event match = match(salesOpen, starts.plus(Duration.ofDays(14)));
        events.create(concert);
        events.publish(concert.id());
        events.create(match);
        events.publish(match.id());
        LOG.info("Seeded 2 published events");
    }

    private static Event concert(Instant salesOpen, Instant starts) {
        return Event.builder()
                .id(Ids.next("EV"))
                .title("Lahore Qawwali Night")
                .description("Nusrat tribute concert at Alhamra.")
                .category("Music")
                .venue(new Venue("VN-alhamra", "Alhamra Hall 1", "Lahore"))
                .startsAt(starts)
                .salesOpenAt(salesOpen)
                .addSection(new Section("VIP", SeatTier.VIP, VIP_ROWS, VIP_SEATS, Money.of("8000")))
                .addSection(new Section("Premium", SeatTier.PREMIUM, PREMIUM_ROWS, PREMIUM_SEATS, Money.of("4500")))
                .addSection(new Section("Standard", SeatTier.STANDARD, STANDARD_ROWS, STANDARD_SEATS, Money.of("2000")))
                .build();
    }

    private static Event match(Instant salesOpen, Instant starts) {
        return Event.builder()
                .id(Ids.next("EV"))
                .title("Pakistan vs Sri Lanka - T20")
                .description("International T20 at Gaddafi Stadium.")
                .category("Sports")
                .venue(new Venue("VN-gaddafi", "Gaddafi Stadium", "Lahore"))
                .startsAt(starts)
                .salesOpenAt(salesOpen)
                .addSection(new Section("Pavilion", SeatTier.VIP, VIP_ROWS, PAVILION_SEATS, Money.of("12000")))
                .addSection(new Section("Enclosure", SeatTier.STANDARD, ENCLOSURE_ROWS, ENCLOSURE_SEATS,
                        Money.of("2500")))
                .build();
    }
}
