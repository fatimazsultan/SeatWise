package com.seatwise.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seatwise.booking.Booking;
import com.seatwise.booking.BookingStatus;
import com.seatwise.booking.Customer;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.MutableClock;
import com.seatwise.payment.PaymentMethod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class AppPersistenceTest {

    @Test
    void sqliteRoundTripSurvivesRestart() throws Exception {
        Path dir = Files.createTempDirectory("seatwise-test");
        Path db = dir.resolve("seatwise.db");
        MutableClock clock = MutableClock.epoch();
        String bookingId;
        String eventId;
        String seatId;
        try (SeatWiseApp app = SeatWiseApp.open(AppConfig.demo(db, clock, Duration.ofMinutes(10)))) {
            Event event = app.events().listUpcoming().get(0);
            eventId = event.id();
            seatId = Seat.idOf(eventId, "VIP", "A", 1);
            Booking confirmed = app.checkout().checkout(eventId,
                    new Customer("Aisha Khan", "aisha@example.com", "03001112233"),
                    List.of(seatId), PaymentMethod.CARD, "4111111111111111");
            bookingId = confirmed.id();
            assertEquals(BookingStatus.CONFIRMED, confirmed.status());
            waitUntil(() -> !app.notifications().all().isEmpty());
            assertFalse(app.notifications().all().isEmpty());
        }
        Thread.sleep(200);
        try (SeatWiseApp again = SeatWiseApp.open(AppConfig.demo(db, clock, Duration.ofMinutes(10)))) {
            assertEquals(BookingStatus.CONFIRMED, again.bookings().getById(bookingId).status());
            assertEquals(SeatStatus.BOOKED, again.inventory().statusOf(eventId, seatId));
            assertEquals(2, again.events().listAll().size());
            assertTrue(again.payments().historyOf(bookingId).get(0).succeeded());
        }
    }

    @Test
    void demoFinishesAndLaunchOptionsParse() throws Exception {
        Path db = Files.createTempDirectory("seatwise-demo").resolve("demo.db");
        DemoRunner.run(db);
        assertTrue(Files.exists(db));
        Main.LaunchOptions demo = Main.LaunchOptions.parse(new String[] {"--demo", "--db", "custom.db"});
        assertTrue(demo.demo());
        assertEquals(Path.of("custom.db"), demo.databaseFile());
        Main.LaunchOptions interactive = Main.LaunchOptions.parse(new String[] {});
        assertFalse(interactive.demo());
        assertEquals(Path.of("data", "seatwise.db"), interactive.databaseFile());
    }

    @Test
    void consoleListsEventsAndExits() throws Exception {
        Path db = Files.createTempDirectory("seatwise-ui").resolve("ui.db");
        MutableClock clock = MutableClock.epoch();
        try (SeatWiseApp app = SeatWiseApp.open(AppConfig.demo(db, clock, Duration.ofMinutes(5)))) {
            String input = "1\n9\n0\n";
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            new ConsoleUI(app, new java.io.ByteArrayInputStream(input.getBytes()),
                    new java.io.PrintStream(buffer)).run();
            String text = buffer.toString();
            assertTrue(text.contains("Lahore Qawwali Night"));
            assertTrue(text.contains("Unknown option"));
            assertTrue(text.contains("Goodbye"));
        }
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
    }
}
