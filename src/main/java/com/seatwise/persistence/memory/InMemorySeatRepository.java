package com.seatwise.persistence.memory;

import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatRepository;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.common.Guard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory seats, used by unit tests. */
public final class InMemorySeatRepository implements SeatRepository {

    private final ConcurrentHashMap<String, Seat> seats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SeatStatus> statuses = new ConcurrentHashMap<>();

    @Override
    public void saveAll(List<Seat> toSave) {
        Guard.notEmpty(toSave, "seats");
        for (Seat seat : toSave) {
            seats.put(seat.id(), seat);
            statuses.put(seat.id(), SeatStatus.AVAILABLE);
        }
    }

    @Override
    public List<Seat> findByEvent(String eventId) {
        Guard.notBlank(eventId, "eventId");
        List<Seat> result = new ArrayList<>();
        for (Seat seat : seats.values()) {
            if (eventId.equals(seat.eventId())) {
                result.add(seat);
            }
        }
        return result;
    }

    @Override
    public Map<String, SeatStatus> findStatusByEvent(String eventId) {
        Map<String, SeatStatus> result = new HashMap<>();
        for (Seat seat : findByEvent(eventId)) {
            result.put(seat.id(), statuses.getOrDefault(seat.id(), SeatStatus.AVAILABLE));
        }
        return result;
    }

    @Override
    public void updateStatus(String seatId, SeatStatus status) {
        Guard.notBlank(seatId, "seatId");
        Guard.notNull(status, "status");
        Guard.that(seats.containsKey(seatId), "unknown seat " + seatId);
        statuses.put(seatId, status);
    }

    @Override
    public void updateStatus(Collection<String> seatIds, SeatStatus status) {
        Guard.notEmpty(seatIds, "seatIds");
        for (String seatId : seatIds) {
            updateStatus(seatId, status);
        }
    }
}
