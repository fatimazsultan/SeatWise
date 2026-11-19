package com.seatwise.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seatwise.common.EntityNotFoundException;
import com.seatwise.common.InvalidStateTransitionException;
import com.seatwise.common.MutableClock;
import com.seatwise.common.ValidationException;
import com.seatwise.persistence.memory.InMemoryEventRepository;
import com.seatwise.persistence.memory.InMemorySeatRepository;
import com.seatwise.support.Fixtures;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Create and publish rules of the catalog module (owner: Ahmed). */
class EventServiceTest {

    private MutableClock clock;
    private EventService service;

    @BeforeEach
    void setUp() {
        clock = Fixtures.clock();
        service = new EventService(new InMemoryEventRepository(), new InMemorySeatRepository(), clock);
    }

    @Test
    void createStoresEventAndGeneratesItsSeatMap() {
        Event event = service.create(Fixtures.unpublishedConcert(clock));
        assertEquals(EventStatus.DRAFT, event.status());
        List<Seat> seats = service.seatsOf(event.id());
        assertEquals(event.capacity(), seats.size());
        assertEquals(16, seats.size());
    }

    @Test
    void createRejectsDuplicateIdPastStartAndNull() {
        service.create(Fixtures.unpublishedConcert(clock));
        assertThrows(ValidationException.class, () -> service.create(Fixtures.unpublishedConcert(clock)));
        assertThrows(ValidationException.class, () -> service.create(null));

        Event early = Fixtures.unpublishedConcert(clock);
        clock.advance(Duration.ofDays(30));
        EventService fresh = new EventService(new InMemoryEventRepository(), new InMemorySeatRepository(), clock);
        assertThrows(ValidationException.class, () -> fresh.create(early));
    }

    @Test
    void publishMovesDraftToPublishedOnlyOnce() {
        Event event = service.create(Fixtures.unpublishedConcert(clock));
        assertEquals(EventStatus.PUBLISHED, service.publish(event.id()).status());
        assertThrows(InvalidStateTransitionException.class, () -> service.publish(event.id()));
    }

    @Test
    void cancelWorksOnceAndIsPersisted() {
        Event event = service.create(Fixtures.unpublishedConcert(clock));
        service.publish(event.id());
        assertEquals(EventStatus.CANCELLED, service.cancel(event.id()).status());
        assertEquals(EventStatus.CANCELLED, service.getById(event.id()).status());
        assertThrows(InvalidStateTransitionException.class, () -> service.cancel(event.id()));
    }

    @Test
    void lookupOfUnknownOrBlankIdFailsWithTypedErrors() {
        assertThrows(EntityNotFoundException.class, () -> service.getById("EV-missing"));
        assertThrows(EntityNotFoundException.class, () -> service.seatsOf("EV-missing"));
        assertThrows(EntityNotFoundException.class, () -> service.publish("EV-missing"));
        assertThrows(ValidationException.class, () -> service.getById(" "));
    }

    @Test
    void upcomingListsOnlyPublishedFutureEvents() {
        Event draft = service.create(Fixtures.unpublishedConcert(clock));
        assertTrue(service.listUpcoming().isEmpty());
        assertEquals(1, service.listAll().size());
        service.publish(draft.id());
        assertEquals(1, service.listUpcoming().size());
        clock.advance(Duration.ofDays(11));
        assertTrue(service.listUpcoming().isEmpty());
    }
}
