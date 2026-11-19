package com.seatwise.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Persistence port for seats. */
public interface SeatRepository {

    void saveAll(List<Seat> seats);

    List<Seat> findByEvent(String eventId);

    Map<String, SeatStatus> findStatusByEvent(String eventId);

    void updateStatus(String seatId, SeatStatus status);

    void updateStatus(Collection<String> seatIds, SeatStatus status);
}
