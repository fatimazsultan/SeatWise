package com.seatwise.booking;

import com.seatwise.booking.state.BookingState;
import com.seatwise.booking.state.BookingStates;
import com.seatwise.common.Guard;
import com.seatwise.common.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A customer's reservation of one or more seats. Lifecycle changes go through the
 * current {@link BookingState}.
 */
public final class Booking {

    public static final int MAX_SEATS_PER_BOOKING = 10;

    private final String id;
    private final String eventId;
    private final Customer customer;
    private final List<String> seatIds;
    private final Instant createdAt;
    private final Money totalAmount;
    private String paymentId;
    private Instant expiresAt;
    private Instant updatedAt;
    private BookingState state;

    public Booking(String id, String eventId, Customer customer, List<String> seatIds, Money totalAmount,
            Instant createdAt) {
        this.id = Guard.notBlank(id, "booking.id");
        this.eventId = Guard.notBlank(eventId, "booking.eventId");
        this.customer = Guard.notNull(customer, "booking.customer");
        Guard.notEmpty(seatIds, "booking.seatIds");
        Guard.inRange(seatIds.size(), 1, MAX_SEATS_PER_BOOKING, "booking.seatIds count");
        Guard.that(seatIds.stream().distinct().count() == seatIds.size(), "booking.seatIds must be unique");
        this.seatIds = List.copyOf(seatIds);
        this.totalAmount = Guard.notNegative(totalAmount, "booking.totalAmount");
        this.createdAt = Guard.notNull(createdAt, "booking.createdAt");
        this.updatedAt = createdAt;
        this.state = BookingStates.PENDING;
    }

    /** Rebuilds a booking loaded from the database without replaying transitions. */
    public static Booking restore(String id, String eventId, Customer customer, List<String> seatIds,
            Money totalAmount, Instant createdAt, BookingStatus status, Instant expiresAt, String paymentId,
            Instant updatedAt) {
        Booking booking = new Booking(id, eventId, customer, seatIds, totalAmount, createdAt);
        booking.state = BookingStates.of(status);
        booking.expiresAt = expiresAt;
        booking.paymentId = paymentId;
        booking.updatedAt = updatedAt == null ? createdAt : updatedAt;
        return booking;
    }

    public void markHeld(Instant newExpiresAt, Instant now) {
        Guard.notNull(newExpiresAt, "expiresAt");
        Guard.that(newExpiresAt.isAfter(now), "hold expiry must be in the future");
        this.state = state.hold();
        this.expiresAt = newExpiresAt;
        touch(now);
    }

    public void confirm(String newPaymentId, Instant now) {
        this.paymentId = Guard.notBlank(newPaymentId, "paymentId");
        this.state = state.confirm();
        this.expiresAt = null;
        touch(now);
    }

    public void cancel(Instant now) {
        this.state = state.cancel();
        this.expiresAt = null;
        touch(now);
    }

    public void expire(Instant now) {
        this.state = state.expire();
        touch(now);
    }

    public boolean isHoldExpired(Instant now) {
        return status() == BookingStatus.HELD && expiresAt != null && !now.isBefore(expiresAt);
    }

    private void touch(Instant now) {
        this.updatedAt = Guard.notNull(now, "now");
    }

    public String id() {
        return id;
    }

    public String eventId() {
        return eventId;
    }

    public Customer customer() {
        return customer;
    }

    public List<String> seatIds() {
        return seatIds;
    }

    public int seatCount() {
        return seatIds.size();
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Optional<String> paymentId() {
        return Optional.ofNullable(paymentId);
    }

    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public BookingStatus status() {
        return state.status();
    }

    @Override
    public String toString() {
        return id + " [" + status() + "] " + seatIds.size() + " seat(s) " + totalAmount + " " + customer.email();
    }
}
