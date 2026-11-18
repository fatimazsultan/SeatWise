package com.seatwise.notification;

import com.seatwise.booking.Booking;
import com.seatwise.booking.event.BookingEvent;
import com.seatwise.booking.event.BookingEventListener;
import com.seatwise.common.Guard;
import com.seatwise.common.Ids;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;

/**
 * Observer of booking events. Writes email, in-app, and optional SMS copies.
 * The booking module does not import this class; it only knows {@link BookingEventListener}.
 */
public class NotificationService implements BookingEventListener {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = Guard.notNull(notifications, "notifications");
        this.clock = Guard.notNull(clock, "clock");
    }

    @Override
    public void onBookingEvent(BookingEvent event) {
        Guard.notNull(event, "event");
        Booking booking = event.booking();
        Message message = messageFor(event);
        save(booking.id(), NotificationChannel.EMAIL, booking.customer().email(), message);
        save(booking.id(), NotificationChannel.IN_APP, booking.customer().email(), message);
        booking.customer().phoneNumber().ifPresent(phone ->
                save(booking.id(), NotificationChannel.SMS, phone, message));
        LOG.info(() -> "Notified " + booking.customer().email() + " of " + event.type());
    }

    public List<Notification> all() {
        return notifications.findAll();
    }

    public List<Notification> forBooking(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return notifications.findByBooking(bookingId);
    }

    private void save(String bookingId, NotificationChannel channel, String recipient, Message message) {
        notifications.save(new Notification(Ids.next("NT"), bookingId, channel, recipient,
                message.subject, message.body, clock.instant(), true));
    }

    private static Message messageFor(BookingEvent event) {
        Booking booking = event.booking();
        return switch (event.type()) {
            case HELD -> new Message("Seats held",
                    "Booking " + booking.id() + " holds " + booking.seatCount() + " seat(s). Pay "
                            + booking.totalAmount() + ".");
            case CONFIRMED -> new Message("Booking confirmed",
                    "Booking " + booking.id() + " is confirmed. Seats: " + String.join(", ", booking.seatIds()));
            case CANCELLED -> new Message("Booking cancelled",
                    "Booking " + booking.id() + " was cancelled. " + event.detail());
            case EXPIRED -> new Message("Hold expired",
                    "Booking " + booking.id() + " expired. The seats were released.");
            case PAYMENT_FAILED -> new Message("Payment failed",
                    "Payment for " + booking.id() + " was declined. " + event.detail());
        };
    }

    private record Message(String subject, String body) {
    }
}
