package com.seatwise.catalog;

import java.util.List;
import java.util.Optional;

/** Persistence port for events. Services depend on this, not on JDBC. */
public interface EventRepository {

    void save(Event event);

    void updateStatus(String eventId, EventStatus status);

    Optional<Event> findById(String eventId);

    List<Event> findAll();
}
