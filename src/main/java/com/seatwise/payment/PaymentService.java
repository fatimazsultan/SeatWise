package com.seatwise.payment;

import com.seatwise.booking.Booking;
import com.seatwise.common.Guard;
import com.seatwise.common.Ids;
import com.seatwise.common.Money;
import com.seatwise.common.PaymentFailedException;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;

/** Charges a booking and stores every attempt, including declines. */
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class.getName());

    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, PaymentGateway gateway, Clock clock) {
        this.payments = Guard.notNull(payments, "payments");
        this.gateway = Guard.notNull(gateway, "gateway");
        this.clock = Guard.notNull(clock, "clock");
    }

    public Payment charge(Booking booking, Money amount, PaymentMethod method, String instrumentRef) {
        Guard.notNull(booking, "booking");
        Guard.notNegative(amount, "amount");
        Guard.that(!amount.isZero(), "amount must be greater than zero");
        Guard.notNull(method, "method");
        Guard.notBlank(instrumentRef, "instrumentRef");
        Guard.that(amount.equals(booking.totalAmount()),
                "Charged amount " + amount + " does not match booking total " + booking.totalAmount());
        GatewayReceipt receipt = gateway.charge(amount, method, instrumentRef);
        Payment payment = toPayment(booking.id(), amount, method, receipt);
        payments.save(payment);
        if (!payment.succeeded()) {
            String reason = payment.failureReason().orElse("declined");
            LOG.warning(() -> "Payment failed for " + booking.id() + ": " + reason);
            throw new PaymentFailedException(reason);
        }
        LOG.info(() -> "Payment " + payment.id() + " captured for " + booking.id());
        return payment;
    }

    public List<Payment> historyOf(String bookingId) {
        Guard.notBlank(bookingId, "bookingId");
        return payments.findByBooking(bookingId);
    }

    private Payment toPayment(String bookingId, Money amount, PaymentMethod method, GatewayReceipt receipt) {
        if (receipt.success()) {
            return new Payment(Ids.next("PAY"), bookingId, method, amount, PaymentStatus.SUCCESS,
                    receipt.reference(), null, clock.instant());
        }
        return new Payment(Ids.next("PAY"), bookingId, method, amount, PaymentStatus.FAILED,
                receipt.reference(), receipt.failureReason(), clock.instant());
    }
}
