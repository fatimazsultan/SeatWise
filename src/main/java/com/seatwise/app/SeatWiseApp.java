package com.seatwise.app;

import com.seatwise.booking.BookingService;
import com.seatwise.booking.HoldExpiryScheduler;
import com.seatwise.booking.SeatInventory;
import com.seatwise.booking.event.AsyncEventPublisher;
import com.seatwise.booking.event.SimpleEventPublisher;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventService;
import com.seatwise.checkout.CheckoutService;
import com.seatwise.common.Guard;
import com.seatwise.common.PersistenceException;
import com.seatwise.notification.NotificationService;
import com.seatwise.payment.PaymentService;
import com.seatwise.persistence.Database;
import com.seatwise.persistence.JdbcBookingRepository;
import com.seatwise.persistence.JdbcEventRepository;
import com.seatwise.persistence.JdbcNotificationRepository;
import com.seatwise.persistence.JdbcPaymentRepository;
import com.seatwise.persistence.JdbcSeatRepository;
import com.seatwise.persistence.SchemaInitializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Composition root. Opens SQLite, wires every module, starts the hold-expiry thread.
 * Close it with try-with-resources.
 */
public final class SeatWiseApp implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SeatWiseApp.class.getName());
    private static final int NOTIFY_THREADS = 2;

    private final Database database;
    private final EventService events;
    private final BookingService bookings;
    private final PaymentService payments;
    private final NotificationService notifications;
    private final CheckoutService checkout;
    private final SeatInventory inventory;
    private final HoldExpiryScheduler expiryScheduler;
    private final AsyncEventPublisher publisher;
    private final ExecutorService notifyExecutor;
    private final AppConfig config;

    private SeatWiseApp(AppParts parts) {
        this.config = parts.config;
        this.database = parts.database;
        this.events = parts.events;
        this.bookings = parts.bookings;
        this.payments = parts.payments;
        this.notifications = parts.notifications;
        this.checkout = parts.checkout;
        this.inventory = parts.inventory;
        this.expiryScheduler = parts.expiryScheduler;
        this.publisher = parts.publisher;
        this.notifyExecutor = parts.notifyExecutor;
    }

    public static SeatWiseApp open(AppConfig config) {
        Guard.notNull(config, "config");
        ensureParent(config.databaseFile());
        Database database = new Database("jdbc:sqlite:" + config.databaseFile().toAbsolutePath());
        SchemaInitializer.apply(database);
        SeatWiseApp app = wire(config, database);
        app.bootstrap();
        return app;
    }

    public EventService events() {
        return events;
    }

    public BookingService bookings() {
        return bookings;
    }

    public PaymentService payments() {
        return payments;
    }

    public NotificationService notifications() {
        return notifications;
    }

    public CheckoutService checkout() {
        return checkout;
    }

    public SeatInventory inventory() {
        return inventory;
    }

    public HoldExpiryScheduler expiryScheduler() {
        return expiryScheduler;
    }

    public AppConfig config() {
        return config;
    }

    @Override
    public void close() {
        expiryScheduler.close();
        publisher.close();
        notifyExecutor.shutdownNow();
        database.close();
        LOG.info("SeatWise shut down");
    }

    private void bootstrap() {
        if (config.seedIfEmpty() && events.listAll().isEmpty()) {
            SeedData.populate(events, config.clock());
        }
        for (Event event : events.listAll()) {
            bookings.loadInventory(event.id());
        }
        expiryScheduler.start();
        LOG.info(() -> "SeatWise ready with " + events.listAll().size() + " event(s)");
    }

    private static SeatWiseApp wire(AppConfig config, Database database) {
        JdbcEventRepository eventRepo = new JdbcEventRepository(database);
        JdbcSeatRepository seatRepo = new JdbcSeatRepository(database);
        EventService events = new EventService(eventRepo, seatRepo, config.clock());
        SeatInventory inventory = new SeatInventory();
        ExecutorService notifyExecutor = Executors.newFixedThreadPool(NOTIFY_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "seatwise-notify");
            thread.setDaemon(true);
            return thread;
        });
        SimpleEventPublisher sync = new SimpleEventPublisher();
        AsyncEventPublisher publisher = new AsyncEventPublisher(sync, notifyExecutor);
        NotificationService notifications = new NotificationService(new JdbcNotificationRepository(database),
                config.clock());
        publisher.subscribe(notifications);
        BookingService bookings = new BookingService(new JdbcBookingRepository(database), eventRepo, seatRepo,
                inventory, publisher, config.clock(), config.holdDuration());
        PaymentService payments = new PaymentService(new JdbcPaymentRepository(database), config.paymentGateway(),
                config.clock());
        CheckoutService checkout = new CheckoutService(events, bookings, payments, publisher, config.clock());
        HoldExpiryScheduler expiry = new HoldExpiryScheduler(bookings, config.expiryInterval());
        return new SeatWiseApp(new AppParts(config, database, events, bookings, payments, notifications, checkout,
                inventory, expiry, publisher, notifyExecutor));
    }

    private static void ensureParent(Path databaseFile) {
        Path parent = databaseFile.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new PersistenceException("Cannot create database directory " + parent, e);
        }
    }

    private record AppParts(AppConfig config, Database database, EventService events, BookingService bookings,
            PaymentService payments, NotificationService notifications, CheckoutService checkout,
            SeatInventory inventory, HoldExpiryScheduler expiryScheduler, AsyncEventPublisher publisher,
            ExecutorService notifyExecutor) {
    }
}
