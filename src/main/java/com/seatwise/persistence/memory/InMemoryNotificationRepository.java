package com.seatwise.persistence.memory;

import com.seatwise.common.Guard;
import com.seatwise.notification.Notification;
import com.seatwise.notification.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory notifications, used by unit tests. */
public final class InMemoryNotificationRepository implements NotificationRepository {

    private final CopyOnWriteArrayList<Notification> store = new CopyOnWriteArrayList<>();

    @Override
    public void save(Notification notification) {
        store.add(Guard.notNull(notification, "notification"));
    }

    @Override
    public List<Notification> findByBooking(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return store.stream().filter(notification -> bookingId.equals(notification.bookingId())).toList();
    }

    @Override
    public List<Notification> findAll() {
        return new ArrayList<>(store);
    }
}
