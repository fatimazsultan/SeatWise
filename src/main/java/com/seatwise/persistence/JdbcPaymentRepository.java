package com.seatwise.persistence;

import com.seatwise.common.Guard;
import com.seatwise.common.Money;
import com.seatwise.payment.Payment;
import com.seatwise.payment.PaymentMethod;
import com.seatwise.payment.PaymentRepository;
import com.seatwise.payment.PaymentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** JDBC adapter for payments. */
public final class JdbcPaymentRepository implements PaymentRepository {

    private static final String INSERT =
            "INSERT INTO payments(id, booking_id, method, amount, status, gateway_ref, failure_reason, processed_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT =
            "SELECT id, booking_id, method, amount, status, gateway_ref, failure_reason, processed_at FROM payments ";

    private final Database db;

    public JdbcPaymentRepository(Database db) {
        this.db = Guard.notNull(db, "db");
    }

    @Override
    public void save(Payment payment) {
        Guard.notNull(payment, "payment");
        db.execute(INSERT, ps -> {
            ps.setString(1, payment.id());
            ps.setString(2, payment.bookingId());
            ps.setString(3, payment.method().name());
            ps.setString(4, payment.amount().toPlainString());
            ps.setString(5, payment.status().name());
            ps.setString(6, payment.gatewayRef().orElse(null));
            ps.setString(7, payment.failureReason().orElse(null));
            ps.setString(8, payment.processedAt().toString());
        });
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        Guard.notBlank(paymentId, "paymentId");
        return db.queryOne(SELECT + "WHERE id = ?", JdbcPaymentRepository::map, ps -> ps.setString(1, paymentId));
    }

    @Override
    public List<Payment> findByBooking(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return db.query(SELECT + "WHERE booking_id = ? ORDER BY processed_at", JdbcPaymentRepository::map,
                ps -> ps.setString(1, bookingId));
    }

    private static Payment map(ResultSet rs) throws SQLException {
        return new Payment(rs.getString("id"), rs.getString("booking_id"),
                PaymentMethod.valueOf(rs.getString("method")), Money.of(rs.getString("amount")),
                PaymentStatus.valueOf(rs.getString("status")), rs.getString("gateway_ref"),
                rs.getString("failure_reason"), Instant.parse(rs.getString("processed_at")));
    }
}
