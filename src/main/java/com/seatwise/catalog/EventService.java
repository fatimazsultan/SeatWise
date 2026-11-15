package com.seatwise.catalog;

import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.Guard;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/** Creates, publishes, cancels and queries events, and generates their seat maps. */
public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());

    private final EventRepository events;
    private final SeatRepository seats;
    private final Clock clock;

    public EventService(EventRepository events, SeatRepository seats, Clock clock) {
        this.events = Guard.notNull(events, "events");
        this.seats = Guard.notNull(seats, "seats");
        this.clock = Guard.notNull(clock, "clock");
    }

    public Event create(Event event) {
        Guard.notNull(event, "event");
        Guard.that(event.startsAt().isAfter(clock.instant()), "event.startsAt must be in the future");
        Guard.that(events.findById(event.id()).isEmpty(), "event with id " + event.id() + " already exists");
        events.save(event);
        List<Seat> seatMap = SeatMapGenerator.generate(event);
        seats.saveAll(seatMap);
        LOG.info(() -> "Created event " + event.id() + " with " + seatMap.size() + " seats");
        return event;
    }

    public Event publish(String eventId) {
        Event event = getById(eventId);
        event.publish();
        events.updateStatus(eventId, event.status());
        LOG.info(() -> "Published event " + eventId);
        return event;
    }

    public Event cancel(String eventId) {
        Event event = getById(eventId);
        event.cancel();
        events.updateStatus(eventId, event.status());
        LOG.warning(() -> "Cancelled event " + eventId);
        return event;
    }

    public Event getById(String eventId) {
        Guard.notBlank(eventId, "eventId");
        return events.findById(eventId).orElseThrow(() -> new EntityNotFoundException("Event", eventId));
    }

    public List<Event> listAll() {
        return events.findAll().stream().sorted(Comparator.comparing(Event::startsAt)).toList();
    }

    public List<Event> listUpcoming() {
        Instant now = clock.instant();
        return listAll().stream()
                .filter(event -> event.status() == EventStatus.PUBLISHED && event.startsAt().isAfter(now))
                .toList();
    }

    public List<Seat> seatsOf(String eventId) {
        getById(eventId);
        return seats.findByEvent(eventId);
    }
}
