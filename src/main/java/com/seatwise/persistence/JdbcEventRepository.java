package com.seatwise.persistence;

import com.seatwise.catalog.Event;
import com.seatwise.catalog.EventRepository;
import com.seatwise.catalog.EventStatus;
import com.seatwise.catalog.Section;
import com.seatwise.catalog.SeatTier;
import com.seatwise.catalog.Venue;
import com.seatwise.common.Guard;
import com.seatwise.common.Money;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC adapter for events, venues and sections. */
public final class JdbcEventRepository implements EventRepository {

    private static final String INSERT_VENUE =
            "INSERT INTO venues(id, name, city) VALUES (?, ?, ?) "
                    + "ON CONFLICT(id) DO UPDATE SET name = excluded.name, city = excluded.city";
    private static final String INSERT_EVENT =
            "INSERT INTO events(id, title, description, category, venue_id, starts_at, sales_open_at, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_SECTION =
            "INSERT INTO sections(event_id, name, tier, rows_count, seats_per_row, base_price) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_STATUS = "UPDATE events SET status = ? WHERE id = ?";
    private static final String SELECT_COLUMNS =
            "SELECT e.id, e.title, e.description, e.category, e.starts_at, e.sales_open_at, e.status, "
                    + "v.id AS venue_id, v.name AS venue_name, v.city AS venue_city, "
                    + "s.name AS section_name, s.tier, s.rows_count, s.seats_per_row, s.base_price "
                    + "FROM events e JOIN venues v ON v.id = e.venue_id "
                    + "JOIN sections s ON s.event_id = e.id ";

    private final Database db;

    public JdbcEventRepository(Database db) {
        this.db = Guard.notNull(db, "db");
    }

    @Override
    public void save(Event event) {
        Guard.notNull(event, "event");
        db.inTransaction(() -> {
            insertVenue(event.venue());
            insertEvent(event);
            for (Section section : event.sections()) {
                insertSection(event.id(), section);
            }
            return null;
        });
    }

    @Override
    public void updateStatus(String eventId, EventStatus status) {
        Guard.notBlank(eventId, "eventId");
        Guard.notNull(status, "status");
        db.execute(UPDATE_STATUS, ps -> {
            ps.setString(1, status.name());
            ps.setString(2, eventId);
        });
    }

    @Override
    public Optional<Event> findById(String eventId) {
        Guard.notBlank(eventId, "eventId");
        String sql = SELECT_COLUMNS + "WHERE e.id = ? ORDER BY s.name";
        List<Event> found = assemble(db.query(sql, JdbcEventRepository::mapRow, ps -> ps.setString(1, eventId)));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    @Override
    public List<Event> findAll() {
        return assemble(db.query(SELECT_COLUMNS + "ORDER BY e.starts_at, s.name", JdbcEventRepository::mapRow));
    }

    private void insertVenue(Venue venue) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(INSERT_VENUE)) {
            ps.setString(1, venue.id());
            ps.setString(2, venue.name());
            ps.setString(3, venue.city());
            ps.executeUpdate();
        }
    }

    private void insertEvent(Event event) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(INSERT_EVENT)) {
            ps.setString(1, event.id());
            ps.setString(2, event.title());
            ps.setString(3, event.description());
            ps.setString(4, event.category());
            ps.setString(5, event.venue().id());
            ps.setString(6, event.startsAt().toString());
            ps.setString(7, event.salesOpenAt().toString());
            ps.setString(8, event.status().name());
            ps.executeUpdate();
        }
    }

    private void insertSection(String eventId, Section section) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(INSERT_SECTION)) {
            ps.setString(1, eventId);
            ps.setString(2, section.name());
            ps.setString(3, section.tier().name());
            ps.setInt(4, section.rows());
            ps.setInt(5, section.seatsPerRow());
            ps.setString(6, section.basePrice().toPlainString());
            ps.executeUpdate();
        }
    }

    private static EventRow mapRow(ResultSet rs) throws SQLException {
        return new EventRow(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                Instant.parse(rs.getString("starts_at")),
                Instant.parse(rs.getString("sales_open_at")),
                EventStatus.valueOf(rs.getString("status")),
                new Venue(rs.getString("venue_id"), rs.getString("venue_name"), rs.getString("venue_city")),
                new Section(rs.getString("section_name"), SeatTier.valueOf(rs.getString("tier")),
                        rs.getInt("rows_count"), rs.getInt("seats_per_row"), Money.of(rs.getString("base_price"))));
    }

    private static List<Event> assemble(List<EventRow> rows) {
        Map<String, Event.Builder> builders = new LinkedHashMap<>();
        for (EventRow row : rows) {
            builders.computeIfAbsent(row.id, id -> builderFor(row)).addSection(row.section);
        }
        List<Event> events = new ArrayList<>();
        for (Event.Builder builder : builders.values()) {
            events.add(builder.build());
        }
        return events;
    }

    private static Event.Builder builderFor(EventRow row) {
        return Event.builder()
                .id(row.id)
                .title(row.title)
                .description(row.description)
                .category(row.category)
                .startsAt(row.startsAt)
                .salesOpenAt(row.salesOpenAt)
                .status(row.status)
                .venue(row.venue);
    }

    private record EventRow(String id, String title, String description, String category, Instant startsAt,
            Instant salesOpenAt, EventStatus status, Venue venue, Section section) {
    }
}
