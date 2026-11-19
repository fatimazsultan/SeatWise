package com.seatwise.persistence.memory;

import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventRepository;
import com.seatwise.catalog.EventStatus;
import com.seatwise.common.Guard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory events, used by unit tests. */
public final class InMemoryEventRepository implements EventRepository {

    private final ConcurrentHashMap<String, Event> store = new ConcurrentHashMap<>();

    @Override
    public void save(Event event) {
        store.put(Guard.notNull(event, "event").id(), event);
    }

    @Override
    public void updateStatus(String eventId, EventStatus status) {
        Guard.notBlank(eventId, "eventId");
        Guard.notNull(status, "status");
        Guard.that(store.containsKey(eventId), "event " + eventId + " is not stored");
    }

    @Override
    public Optional<Event> findById(String eventId) {
        return Optional.ofNullable(store.get(eventId));
    }

    @Override
    public List<Event> findAll() {
        return new ArrayList<>(store.values());
    }
}
