package com.seatwise.app;

import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Renders a seat map as text. "." available, "h" held, "X" booked. */
public final class SeatMapFormatter {

    private SeatMapFormatter() {
    }

    public static String format(List<Seat> seats, Map<String, SeatStatus> statuses) {
        Map<String, List<Seat>> bySection = group(seats);
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, List<Seat>> entry : bySection.entrySet()) {
            out.append("  Section ").append(entry.getKey()).append('\n');
            appendSection(out, entry.getValue(), statuses);
        }
        out.append("  Legend: . available   h held   X booked\n");
        return out.toString();
    }

    private static Map<String, List<Seat>> group(List<Seat> seats) {
        List<Seat> sorted = new ArrayList<>(seats);
        sorted.sort(Comparator.comparing(Seat::section).thenComparing(Seat::row).thenComparingInt(Seat::number));
        Map<String, List<Seat>> bySection = new LinkedHashMap<>();
        for (Seat seat : sorted) {
            bySection.computeIfAbsent(seat.section(), key -> new ArrayList<>()).add(seat);
        }
        return bySection;
    }

    private static void appendSection(StringBuilder out, List<Seat> seats, Map<String, SeatStatus> statuses) {
        String currentRow = "";
        for (Seat seat : seats) {
            if (!seat.row().equals(currentRow)) {
                if (!currentRow.isEmpty()) {
                    out.append('\n');
                }
                currentRow = seat.row();
                out.append("    ").append(currentRow).append(" | ");
            }
            out.append(glyph(statuses.getOrDefault(seat.id(), SeatStatus.AVAILABLE)));
            out.append(seat.number()).append(' ');
        }
        if (!currentRow.isEmpty()) {
            out.append('\n');
        }
    }

    private static char glyph(SeatStatus status) {
        return switch (status) {
            case AVAILABLE -> '.';
            case HELD -> 'h';
            case BOOKED -> 'X';
        };
    }
}
