package com.seatwise.booking.event;

import com.seatwise.common.Guard;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Synchronous publisher. A failing listener is isolated and cannot break the booking. */
public class SimpleEventPublisher implements BookingEventPublisher {

    private static final Logger LOG = Logger.getLogger(SimpleEventPublisher.class.getName());
    private final CopyOnWriteArrayList<BookingEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void subscribe(BookingEventListener listener) {
        listeners.addIfAbsent(Guard.notNull(listener, "listener"));
    }

    @Override
    public void unsubscribe(BookingEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void publish(BookingEvent event) {
        Guard.notNull(event, "event");
        for (BookingEventListener listener : listeners) {
            deliver(listener, event);
        }
    }

    private static void deliver(BookingEventListener listener, BookingEvent event) {
        try {
            listener.onBookingEvent(event);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Listener failed for " + event.type(), e);
        }
    }

    public int listenerCount() {
        return listeners.size();
    }
}
