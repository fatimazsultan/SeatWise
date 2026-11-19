package com.seatwise.persistence;

import com.seatwise.catalog.Seat;
import com.seatwise.catalog.SeatRepository;
import com.seatwise.catalog.SeatStatus;
import com.seatwise.catalog.SeatTier;
import com.seatwise.common.Guard;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JDBC adapter for seats. */
public final class JdbcSeatRepository implements SeatRepository {

    private static final String INSERT =
            "INSERT INTO seats(id, event_id, section, row_label, number, tier, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT =
            "SELECT id, event_id, section, row_label, number, tier, status FROM seats "
                    + "WHERE event_id = ? ORDER BY section, row_label, number";
    private static final String UPDATE = "UPDATE seats SET status = ? WHERE id = ?";

    private final Database db;

    public JdbcSeatRepository(Database db) {
        this.db = Guard.notNull(db, "db");
    }

    @Override
    public void saveAll(List<Seat> seats) {
        Guard.notEmpty(seats, "seats");
        db.inTransaction(() -> {
            for (Seat seat : seats) {
                insert(seat);
            }
            return null;
        });
    }

    @Override
    public List<Seat> findByEvent(String eventId) {
        Guard.notBlank(eventId, "eventId");
        return db.query(SELECT, JdbcSeatRepository::mapSeat, ps -> ps.setString(1, eventId));
    }

    @Override
    public Map<String, SeatStatus> findStatusByEvent(String eventId) {
        Guard.notBlank(eventId, "eventId");
        Map<String, SeatStatus> statuses = new HashMap<>();
        for (SeatRow row : db.query(SELECT, JdbcSeatRepository::mapRow, ps -> ps.setString(1, eventId))) {
            statuses.put(row.id, row.status);
        }
        return statuses;
    }

    @Override
    public void updateStatus(String seatId, SeatStatus status) {
        Guard.notBlank(seatId, "seatId");
        Guard.notNull(status, "status");
        db.execute(UPDATE, ps -> {
            ps.setString(1, status.name());
            ps.setString(2, seatId);
        });
    }

    @Override
    public void updateStatus(Collection<String> seatIds, SeatStatus status) {
        Guard.notEmpty(seatIds, "seatIds");
        Guard.notNull(status, "status");
        db.inTransaction(() -> {
            for (String seatId : seatIds) {
                updateOne(seatId, status);
            }
            return null;
        });
    }

    private void insert(Seat seat) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(INSERT)) {
            ps.setString(1, seat.id());
            ps.setString(2, seat.eventId());
            ps.setString(3, seat.section());
            ps.setString(4, seat.row());
            ps.setInt(5, seat.number());
            ps.setString(6, seat.tier().name());
            ps.setString(7, SeatStatus.AVAILABLE.name());
            ps.executeUpdate();
        }
    }

    private void updateOne(String seatId, SeatStatus status) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(UPDATE)) {
            ps.setString(1, status.name());
            ps.setString(2, seatId);
            ps.executeUpdate();
        }
    }

    private static Seat mapSeat(ResultSet rs) throws SQLException {
        SeatRow row = mapRow(rs);
        return new Seat(row.id, row.eventId, row.section, row.row, row.number, row.tier);
    }

    private static SeatRow mapRow(ResultSet rs) throws SQLException {
        return new SeatRow(rs.getString("id"), rs.getString("event_id"), rs.getString("section"),
                rs.getString("row_label"), rs.getInt("number"), SeatTier.valueOf(rs.getString("tier")),
                SeatStatus.valueOf(rs.getString("status")));
    }

    private record SeatRow(String id, String eventId, String section, String row, int number, SeatTier tier,
            SeatStatus status) {
    }
}
