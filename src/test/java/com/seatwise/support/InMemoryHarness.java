package com.seatwise.support;

import com.seatwise.booking.BookingService;
import com.seatwise.booking.SeatInventory;
import com.seatwise.booking.event.SimpleEventPublisher;
import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventService;
import com.seatwise.checkout.CheckoutService;
import com.seatwise.common.MutableClock;
import com.seatwise.notification.NotificationService;
import com.seatwise.payment.PaymentGateway;
import com.seatwise.payment.PaymentService;
import com.seatwise.payment.ScriptedPaymentGateway;
import com.seatwise.persistence.memory.InMemoryBookingRepository;
import com.seatwise.persistence.memory.InMemoryEventRepository;
import com.seatwise.persistence.memory.InMemoryNotificationRepository;
import com.seatwise.persistence.memory.InMemoryPaymentRepository;
import com.seatwise.persistence.memory.InMemorySeatRepository;
import java.time.Duration;

/** In-memory wiring used by unit tests. */
public final class InMemoryHarness {

    public final MutableClock clock;
    public final EventService events;
    public final BookingService bookings;
    public final PaymentService payments;
    public final NotificationService notifications;
    public final CheckoutService checkout;
    public final SeatInventory inventory;
    public final SimpleEventPublisher publisher;

    private InMemoryHarness(MutableClock clock, EventService events, BookingService bookings, PaymentService payments,
            NotificationService notifications, CheckoutService checkout, SeatInventory inventory,
            SimpleEventPublisher publisher) {
        this.clock = clock;
        this.events = events;
        this.bookings = bookings;
        this.payments = payments;
        this.notifications = notifications;
        this.checkout = checkout;
        this.inventory = inventory;
        this.publisher = publisher;
    }

    public static InMemoryHarness create() {
        return create(new ScriptedPaymentGateway());
    }

    public static InMemoryHarness create(PaymentGateway gateway) {
        MutableClock clock = Fixtures.clock();
        InMemoryEventRepository eventRepo = new InMemoryEventRepository();
        InMemorySeatRepository seatRepo = new InMemorySeatRepository();
        SimpleEventPublisher publisher = new SimpleEventPublisher();
        NotificationService notifications = new NotificationService(new InMemoryNotificationRepository(), clock);
        publisher.subscribe(notifications);
        EventService events = new EventService(eventRepo, seatRepo, clock);
        SeatInventory inventory = new SeatInventory();
        BookingService bookings = new BookingService(new InMemoryBookingRepository(seatRepo), eventRepo, seatRepo,
                inventory, publisher, clock, Duration.ofMinutes(10));
        PaymentService payments = new PaymentService(new InMemoryPaymentRepository(), gateway, clock);
        CheckoutService checkout = new CheckoutService(events, bookings, payments, publisher, clock);
        return new InMemoryHarness(clock, events, bookings, payments, notifications, checkout, inventory, publisher);
    }

    public Event publishedConcert() {
        Event event = Fixtures.unpublishedConcert(clock);
        events.create(event);
        events.publish(event.id());
        bookings.loadInventory(event.id());
        return events.getById(event.id());
    }
}
