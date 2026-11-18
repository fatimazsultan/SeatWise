package com.seatwise.persistence;

import com.seatwise.booking.Booking;
import com.seatwise.booking.BookingRepository;
import com.seatwise.booking.BookingStatus;
import com.seatwise.booking.Customer;
import com.seatwise.catalog.SeatStatus;
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

/** JDBC adapter for bookings. Booking rows and seat statuses commit together. */
public final class JdbcBookingRepository implements BookingRepository {

    private static final String INSERT_BOOKING =
            "INSERT INTO bookings(id, event_id, customer_name, customer_email, customer_phone, status, "
                    + "total_amount, payment_id, created_at, expires_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_SEAT = "INSERT INTO booking_seats(booking_id, seat_id) VALUES (?, ?)";
    private static final String UPDATE_BOOKING =
            "UPDATE bookings SET status = ?, payment_id = ?, expires_at = ?, updated_at = ? WHERE id = ?";
    private static final String UPDATE_SEAT = "UPDATE seats SET status = ? WHERE id = ?";
    private static final String SELECT =
            "SELECT b.id, b.event_id, b.customer_name, b.customer_email, b.customer_phone, b.status, "
                    + "b.total_amount, b.payment_id, b.created_at, b.expires_at, b.updated_at, bs.seat_id "
                    + "FROM bookings b JOIN booking_seats bs ON bs.booking_id = b.id ";

    private final Database db;

    public JdbcBookingRepository(Database db) {
        this.db = Guard.notNull(db, "db");
    }

    @Override
    public void saveHold(Booking booking) {
        Guard.notNull(booking, "booking");
        db.inTransaction(() -> {
            insertBooking(booking);
            for (String seatId : booking.seatIds()) {
                linkSeat(booking.id(), seatId);
                setSeatStatus(seatId, SeatStatus.HELD);
            }
            return null;
        });
    }

    @Override
    public void update(Booking booking, SeatStatus seatStatus) {
        Guard.notNull(booking, "booking");
        Guard.notNull(seatStatus, "seatStatus");
        db.inTransaction(() -> {
            updateBooking(booking);
            for (String seatId : booking.seatIds()) {
                setSeatStatus(seatId, seatStatus);
            }
            return null;
        });
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        List<Booking> found = assemble(db.query(SELECT + "WHERE b.id = ? ORDER BY bs.seat_id",
                JdbcBookingRepository::mapRow, ps -> ps.setString(1, bookingId)));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    @Override
    public List<Booking> findByEvent(String eventId) {
        Guard.notBlank(eventId, "eventId");
        return assemble(db.query(SELECT + "WHERE b.event_id = ? ORDER BY b.created_at, bs.seat_id",
                JdbcBookingRepository::mapRow, ps -> ps.setString(1, eventId)));
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        Guard.notNull(status, "status");
        return assemble(db.query(SELECT + "WHERE b.status = ? ORDER BY b.created_at, bs.seat_id",
                JdbcBookingRepository::mapRow, ps -> ps.setString(1, status.name())));
    }

    @Override
    public List<Booking> findByCustomerEmail(String email) {
        Guard.notBlank(email, "email");
        return assemble(db.query(SELECT + "WHERE b.customer_email = ? ORDER BY b.created_at, bs.seat_id",
                JdbcBookingRepository::mapRow, ps -> ps.setString(1, email)));
    }

    @Override
    public List<Booking> findAll() {
        return assemble(db.query(SELECT + "ORDER BY b.created_at, bs.seat_id", JdbcBookingRepository::mapRow));
    }

    private void insertBooking(Booking booking) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(INSERT_BOOKING)) {
            ps.setString(1, booking.id());
            ps.setString(2, booking.eventId());
            ps.setString(3, booking.customer().name());
            ps.setString(4, booking.customer().email());
            ps.setString(5, booking.customer().phone());
            ps.setString(6, booking.status().name());
            ps.setString(7, booking.totalAmount().toPlainString());
            ps.setString(8, booking.paymentId().orElse(null));
            ps.setString(9, booking.createdAt().toString());
            ps.setString(10, booking.expiresAt().map(Instant::toString).orElse(null));
            ps.setString(11, booking.updatedAt().toString());
            ps.executeUpdate();
        }
    }

    private void linkSeat(String bookingId, String seatId) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(INSERT_SEAT)) {
            ps.setString(1, bookingId);
            ps.setString(2, seatId);
            ps.executeUpdate();
        }
    }

    private void updateBooking(Booking booking) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(UPDATE_BOOKING)) {
            ps.setString(1, booking.status().name());
            ps.setString(2, booking.paymentId().orElse(null));
            ps.setString(3, booking.expiresAt().map(Instant::toString).orElse(null));
            ps.setString(4, booking.updatedAt().toString());
            ps.setString(5, booking.id());
            ps.executeUpdate();
        }
    }

    private void setSeatStatus(String seatId, SeatStatus status) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement(UPDATE_SEAT)) {
            ps.setString(1, status.name());
            ps.setString(2, seatId);
            ps.executeUpdate();
        }
    }

    private static BookingJoin mapRow(ResultSet rs) throws SQLException {
        String expires = rs.getString("expires_at");
        return new BookingJoin(
                rs.getString("id"),
                rs.getString("event_id"),
                new Customer(rs.getString("customer_name"), rs.getString("customer_email"),
                        rs.getString("customer_phone")),
                BookingStatus.valueOf(rs.getString("status")),
                Money.of(rs.getString("total_amount")),
                Instant.parse(rs.getString("created_at")),
                expires == null ? null : Instant.parse(expires),
                rs.getString("payment_id"),
                Instant.parse(rs.getString("updated_at")),
                rs.getString("seat_id"));
    }

    private static List<Booking> assemble(List<BookingJoin> rows) {
        Map<String, List<BookingJoin>> grouped = new LinkedHashMap<>();
        for (BookingJoin row : rows) {
            grouped.computeIfAbsent(row.id, key -> new ArrayList<>()).add(row);
        }
        List<Booking> bookings = new ArrayList<>();
        for (List<BookingJoin> group : grouped.values()) {
            bookings.add(toBooking(group));
        }
        return bookings;
    }

    private static Booking toBooking(List<BookingJoin> group) {
        BookingJoin first = group.get(0);
        List<String> seatIds = new ArrayList<>();
        for (BookingJoin row : group) {
            seatIds.add(row.seatId);
        }
        return Booking.restore(first.id, first.eventId, first.customer, seatIds, first.total, first.createdAt,
                first.status, first.expiresAt, first.paymentId, first.updatedAt);
    }

    private record BookingJoin(String id, String eventId, Customer customer, BookingStatus status, Money total,
            Instant createdAt, Instant expiresAt, String paymentId, Instant updatedAt, String seatId) {
    }
}
