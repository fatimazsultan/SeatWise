package com.seatwise.booking;

import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.Guard;
import com.seatwise.common.SeatUnavailableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory source of truth for seat availability.
 *
 * <p>Thread safety: each event has its own {@link ReentrantLock}. A multi-seat hold is
 * all-or-nothing under that one lock. A thread never holds two event locks, so this
 * class cannot deadlock with itself. Metrics use {@link AtomicLong}.</p>
 */
public final class SeatInventory {

    private final ConcurrentHashMap<String, EventSeats> byEvent = new ConcurrentHashMap<>();
    private final AtomicLong holdAttempts = new AtomicLong();
    private final AtomicLong holdConflicts = new AtomicLong();

    public void load(String eventId, Map<String, SeatStatus> statuses) {
        Guard.notBlank(eventId, "eventId");
        Guard.notNull(statuses, "statuses");
        EventSeats eventSeats = byEvent.computeIfAbsent(eventId, key -> new EventSeats());
        eventSeats.lock.lock();
        try {
            eventSeats.status.clear();
            eventSeats.status.putAll(statuses);
        } finally {
            eventSeats.lock.unlock();
        }
    }

    public boolean hasEvent(String eventId) {
        return byEvent.containsKey(eventId);
    }

    public void hold(String eventId, Collection<String> seatIds) {
        EventSeats eventSeats = eventSeats(eventId);
        Guard.notEmpty(seatIds, "seatIds");
        holdAttempts.incrementAndGet();
        eventSeats.lock.lock();
        try {
            List<String> unavailable = findUnavailable(eventSeats, seatIds);
            if (!unavailable.isEmpty()) {
                holdConflicts.incrementAndGet();
                throw new SeatUnavailableException(unavailable);
            }
            putAll(eventSeats, seatIds, SeatStatus.HELD);
        } finally {
            eventSeats.lock.unlock();
        }
    }

    public void confirm(String eventId, Collection<String> seatIds) {
        transition(eventId, seatIds, SeatStatus.HELD, SeatStatus.BOOKED);
    }

    public void undoConfirm(String eventId, Collection<String> seatIds) {
        transition(eventId, seatIds, SeatStatus.BOOKED, SeatStatus.HELD);
    }

    public void release(String eventId, Collection<String> seatIds) {
        transition(eventId, seatIds, SeatStatus.HELD, SeatStatus.AVAILABLE);
    }

    public void releaseBooked(String eventId, Collection<String> seatIds) {
        transition(eventId, seatIds, SeatStatus.BOOKED, SeatStatus.AVAILABLE);
    }

    public SeatStatus statusOf(String eventId, String seatId) {
        EventSeats eventSeats = eventSeats(eventId);
        eventSeats.lock.lock();
        try {
            return requireStatus(eventSeats, seatId);
        } finally {
            eventSeats.lock.unlock();
        }
    }

    public Map<String, SeatStatus> snapshot(String eventId) {
        EventSeats eventSeats = eventSeats(eventId);
        eventSeats.lock.lock();
        try {
            return new HashMap<>(eventSeats.status);
        } finally {
            eventSeats.lock.unlock();
        }
    }

    public Map<SeatStatus, Integer> counts(String eventId) {
        Map<SeatStatus, Integer> counts = new EnumMap<>(SeatStatus.class);
        for (SeatStatus status : SeatStatus.values()) {
            counts.put(status, 0);
        }
        for (SeatStatus status : snapshot(eventId).values()) {
            counts.merge(status, 1, Integer::sum);
        }
        return counts;
    }

    public long holdAttempts() {
        return holdAttempts.get();
    }

    public long holdConflicts() {
        return holdConflicts.get();
    }

    private void transition(String eventId, Collection<String> seatIds, SeatStatus from, SeatStatus to) {
        EventSeats eventSeats = eventSeats(eventId);
        Guard.notEmpty(seatIds, "seatIds");
        eventSeats.lock.lock();
        try {
            for (String seatId : seatIds) {
                SeatStatus current = requireStatus(eventSeats, seatId);
                if (current != from) {
                    throw new IllegalStateException("Seat " + seatId + " expected " + from + " but was " + current);
                }
            }
            putAll(eventSeats, seatIds, to);
        } finally {
            eventSeats.lock.unlock();
        }
    }

    private static List<String> findUnavailable(EventSeats eventSeats, Collection<String> seatIds) {
        List<String> unavailable = new ArrayList<>();
        for (String seatId : seatIds) {
            SeatStatus current = requireStatus(eventSeats, seatId);
            if (current != SeatStatus.AVAILABLE) {
                unavailable.add(seatId);
            }
        }
        return unavailable;
    }

    private static SeatStatus requireStatus(EventSeats eventSeats, String seatId) {
        SeatStatus current = eventSeats.status.get(seatId);
        if (current == null) {
            throw new EntityNotFoundException("Seat", seatId);
        }
        return current;
    }

    private static void putAll(EventSeats eventSeats, Collection<String> seatIds, SeatStatus status) {
        for (String seatId : seatIds) {
            eventSeats.status.put(seatId, status);
        }
    }

    private EventSeats eventSeats(String eventId) {
        Guard.notBlank(eventId, "eventId");
        EventSeats eventSeats = byEvent.get(eventId);
        if (eventSeats == null) {
            throw new EntityNotFoundException("Event inventory", eventId);
        }
        return eventSeats;
    }

    private static final class EventSeats {
        private final ReentrantLock lock = new ReentrantLock();
        private final Map<String, SeatStatus> status = new HashMap<>();
    }
}
