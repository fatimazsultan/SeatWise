package com.seatwise.payment;

import java.util.List;
import java.util.Optional;

/** Persistence port for payments. */
public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findById(String paymentId);

    List<Payment> findByBooking(String bookingId);
}
