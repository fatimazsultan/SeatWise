package com.seatwise.app;

import com.seatwise.booking.Booking;
import com.seatwise.booking.Customer;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.SeatWiseException;
import com.seatwise.payment.PaymentMethod;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Menu-driven console for the live demo. */
public final class ConsoleUI {

    private final SeatWiseApp app;
    private final Scanner in;
    private final PrintStream out;
    private final Map<String, Runnable> actions = new LinkedHashMap<>();

    public ConsoleUI(SeatWiseApp app, InputStream input, PrintStream out) {
        this.app = app;
        this.in = new Scanner(input);
        this.out = out;
        actions.put("1", this::listEvents);
        actions.put("2", this::showSeatMap);
        actions.put("3", this::holdSeats);
        actions.put("4", this::payAndConfirm);
        actions.put("5", this::cancelBooking);
        actions.put("6", this::lookupBookings);
        actions.put("7", this::listNotifications);
        actions.put("0", this::exitChosen);
    }

    public void run() {
        out.println("SeatWise - Event Ticketing and Seat Reservation");
        String choice = "";
        while (!"0".equals(choice)) {
            printMenu();
            choice = in.nextLine().trim();
            Runnable action = actions.get(choice);
            if (action == null) {
                out.println("Unknown option.");
            } else {
                runSafely(action);
            }
        }
        out.println("Goodbye.");
    }

    private void exitChosen() {
        out.flush();
    }

    private void printMenu() {
        out.println();
        out.println("1) List upcoming events");
        out.println("2) Show seat map");
        out.println("3) Hold seats");
        out.println("4) Pay and confirm a hold");
        out.println("5) Cancel a booking");
        out.println("6) Look up bookings by email");
        out.println("7) Show notifications");
        out.println("0) Exit");
        out.print("> ");
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (SeatWiseException e) {
            out.println("Error [" + e.errorCode() + "]: " + e.getMessage());
        }
    }

    private void listEvents() {
        List<Event> events = app.events().listUpcoming();
        if (events.isEmpty()) {
            out.println("No upcoming events.");
            return;
        }
        for (Event event : events) {
            out.println("  " + event.id() + "  " + event.title() + "  @ " + event.venue().name()
                    + "  capacity " + event.capacity());
        }
    }

    private void showSeatMap() {
        Event event = app.events().getById(prompt("Event id"));
        List<Seat> seats = app.events().seatsOf(event.id());
        Map<String, SeatStatus> statuses = app.inventory().snapshot(event.id());
        out.println(SeatMapFormatter.format(seats, statuses));
        out.println("  counts " + app.inventory().counts(event.id()));
    }

    private void holdSeats() {
        Event event = app.events().getById(prompt("Event id"));
        Customer customer = new Customer(prompt("Your name"), prompt("Email"), blankToNull(prompt("Phone (optional)")));
        Booking held = app.checkout().hold(event.id(), customer, readSeatIds(event.id()));
        out.println("Held " + held.id() + " until " + held.expiresAt().orElse(null) + " total " + held.totalAmount());
        out.println("Pay with option 4. A card ending in 0000 is declined.");
    }

    private void payAndConfirm() {
        String bookingId = prompt("Booking id");
        PaymentMethod method = PaymentMethod.valueOf(prompt("Method CARD/WALLET/BANK_TRANSFER").toUpperCase());
        Booking confirmed = app.checkout().payAndConfirm(bookingId, method, prompt("Card or account ref"));
        out.println("Confirmed " + confirmed.id() + " payment " + confirmed.paymentId().orElse(""));
    }

    private void cancelBooking() {
        Booking cancelled = app.bookings().cancel(prompt("Booking id"), "customer request");
        out.println("Cancelled " + cancelled.id() + " status=" + cancelled.status());
    }

    private void lookupBookings() {
        List<Booking> found = app.bookings().findByCustomerEmail(prompt("Customer email"));
        if (found.isEmpty()) {
            out.println("No bookings.");
            return;
        }
        for (Booking booking : found) {
            out.println("  " + booking);
        }
    }

    private void listNotifications() {
        app.notifications().all().forEach(note -> out.println("  " + note));
    }

    private List<String> readSeatIds(String eventId) {
        out.println("Seat labels, comma-separated. Example: VIP-A-1,VIP-A-2");
        List<String> ids = new ArrayList<>();
        for (String token : prompt("Seats").split(",")) {
            String label = token.trim();
            if (!label.isEmpty()) {
                ids.add(label.contains(":") ? label : eventId + ":" + label);
            }
        }
        return ids;
    }

    private String prompt(String label) {
        out.print(label + ": ");
        return in.nextLine().trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
