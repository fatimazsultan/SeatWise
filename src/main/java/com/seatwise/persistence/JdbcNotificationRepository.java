package com.seatwise.persistence;

import com.seatwise.common.Guard;
import com.seatwise.notification.Notification;
import com.seatwise.notification.NotificationChannel;
import com.seatwise.notification.NotificationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/** JDBC adapter for notifications. */
public final class JdbcNotificationRepository implements NotificationRepository {

    private static final String INSERT =
            "INSERT INTO notifications(id, booking_id, channel, recipient, subject, body, sent_at, delivered) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT =
            "SELECT id, booking_id, channel, recipient, subject, body, sent_at, delivered FROM notifications ";

    private final Database db;

    public JdbcNotificationRepository(Database db) {
        this.db = Guard.notNull(db, "db");
    }

    @Override
    public void save(Notification notification) {
        Guard.notNull(notification, "notification");
        db.execute(INSERT, ps -> {
            ps.setString(1, notification.id());
            ps.setString(2, notification.bookingId());
            ps.setString(3, notification.channel().name());
            ps.setString(4, notification.recipient());
            ps.setString(5, notification.subject());
            ps.setString(6, notification.body());
            ps.setString(7, notification.sentAt().toString());
            ps.setInt(8, notification.delivered() ? 1 : 0);
        });
    }

    @Override
    public List<Notification> findByBooking(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return db.query(SELECT + "WHERE booking_id = ? ORDER BY sent_at", JdbcNotificationRepository::map,
                ps -> ps.setString(1, bookingId));
    }

    @Override
    public List<Notification> findAll() {
        return db.query(SELECT + "ORDER BY sent_at", JdbcNotificationRepository::map);
    }

    private static Notification map(ResultSet rs) throws SQLException {
        return new Notification(rs.getString("id"), rs.getString("booking_id"),
                NotificationChannel.valueOf(rs.getString("channel")), rs.getString("recipient"),
                rs.getString("subject"), rs.getString("body"), Instant.parse(rs.getString("sent_at")),
                rs.getInt("delivered") == 1);
    }
}
