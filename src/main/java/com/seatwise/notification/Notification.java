package com.seatwise.notification;

import com.seatwise.common.Guard;
import java.time.Instant;

/** One outbound message produced from a booking event. */
public final class Notification {

    private final String id;
    private final String bookingId;
    private final NotificationChannel channel;
    private final String recipient;
    private final String subject;
    private final String body;
    private final Instant sentAt;
    private final boolean delivered;

    public Notification(String id, String bookingId, NotificationChannel channel, String recipient,
            String subject, String body, Instant sentAt, boolean delivered) {
        this.id = Guard.notBlank(id, "notification.id");
        this.bookingId = Guard.notBlank(bookingId, "notification.bookingId");
        this.channel = Guard.notNull(channel, "notification.channel");
        this.recipient = Guard.notBlank(recipient, "notification.recipient");
        this.subject = Guard.notBlank(subject, "notification.subject");
        this.body = Guard.notNull(body, "notification.body");
        this.sentAt = Guard.notNull(sentAt, "notification.sentAt");
        this.delivered = delivered;
    }

    public String id() {
        return id;
    }

    public String bookingId() {
        return bookingId;
    }

    public NotificationChannel channel() {
        return channel;
    }

    public String recipient() {
        return recipient;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public Instant sentAt() {
        return sentAt;
    }

    public boolean delivered() {
        return delivered;
    }

    @Override
    public String toString() {
        return "[" + channel + "] " + subject + " -> " + recipient;
    }
}
