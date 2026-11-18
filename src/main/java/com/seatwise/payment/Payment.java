package com.seatwise.payment;

import com.seatwise.common.Guard;
import com.seatwise.common.Money;
import java.time.Instant;
import java.util.Optional;

/** One recorded charge attempt, successful or not. */
public final class Payment {

    private final String id;
    private final String bookingId;
    private final PaymentMethod method;
    private final Money amount;
    private final PaymentStatus status;
    private final String gatewayRef;
    private final String failureReason;
    private final Instant processedAt;

    public Payment(String id, String bookingId, PaymentMethod method, Money amount, PaymentStatus status,
            String gatewayRef, String failureReason, Instant processedAt) {
        this.id = Guard.notBlank(id, "payment.id");
        this.bookingId = Guard.notBlank(bookingId, "payment.bookingId");
        this.method = Guard.notNull(method, "payment.method");
        this.amount = Guard.notNegative(amount, "payment.amount");
        this.status = Guard.notNull(status, "payment.status");
        this.processedAt = Guard.notNull(processedAt, "payment.processedAt");
        this.gatewayRef = gatewayRef;
        this.failureReason = failureReason;
        if (status == PaymentStatus.FAILED) {
            Guard.notBlank(failureReason, "payment.failureReason");
        }
    }

    public String id() {
        return id;
    }

    public String bookingId() {
        return bookingId;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }

    public Optional<String> gatewayRef() {
        return Optional.ofNullable(gatewayRef);
    }

    public Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    public Instant processedAt() {
        return processedAt;
    }

    public boolean succeeded() {
        return status == PaymentStatus.SUCCESS;
    }
}
