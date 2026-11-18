package com.seatwise.persistence.memory;

import com.seatwise.common.Guard;
import com.seatwise.payment.Payment;
import com.seatwise.payment.PaymentRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory payments, used by unit tests. */
public final class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<String, Payment> store = new ConcurrentHashMap<>();

    @Override
    public void save(Payment payment) {
        store.put(Guard.notNull(payment, "payment").id(), payment);
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    @Override
    public List<Payment> findByBooking(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return store.values().stream().filter(payment -> bookingId.equals(payment.bookingId())).toList();
    }
}
