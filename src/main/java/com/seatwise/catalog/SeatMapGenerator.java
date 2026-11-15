package com.seatwise.catalog;

import com.seatwise.common.Guard;
import java.util.ArrayList;
import java.util.List;

/** Expands an event's sections into concrete seats. */
public final class SeatMapGenerator {

    private SeatMapGenerator() {
    }

    public static List<Seat> generate(Event event) {
        Guard.notNull(event, "event");
        List<Seat> seats = new ArrayList<>();
        for (Section section : event.sections()) {
            addSection(seats, event.id(), section);
        }
        return List.copyOf(seats);
    }

    private static void addSection(List<Seat> seats, String eventId, Section section) {
        for (int rowIndex = 0; rowIndex < section.rows(); rowIndex++) {
            String row = String.valueOf((char) ('A' + rowIndex));
            for (int number = 1; number <= section.seatsPerRow(); number++) {
                String id = Seat.idOf(eventId, section.name(), row, number);
                seats.add(new Seat(id, eventId, section.name(), row, number, section.tier()));
            }
        }
    }
}
