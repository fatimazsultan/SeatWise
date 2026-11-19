package com.seatwise.notification;

import java.util.List;

/** Persistence port for outbound notifications. */
public interface NotificationRepository {

    void save(Notification notification);

    List<Notification> findByBooking(String bookingId);

    List<Notification> findAll();
}
