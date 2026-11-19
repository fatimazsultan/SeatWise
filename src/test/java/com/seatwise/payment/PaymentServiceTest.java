package com.seatwise.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seatwise.booking.Booking;
import com.seatwise.booking.Customer;
import com.seatwise.common.Money;
import com.seatwise.common.PaymentFailedException;
import com.seatwise.common.ValidationException;
import com.seatwise.support.Fixtures;
import com.seatwise.support.InMemoryHarness;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Charge rules and gateway strategies of the payments module (owner: Abdul). */
class PaymentServiceTest {

    private static Booking heldBooking(InMemoryHarness app) {
        String eventId = app.publishedConcert().id();
        return app.checkout.hold(eventId, new Customer("Aisha", "aisha@example.com"),
                List.of(Fixtures.seat("Std", "A", 1)));
    }

    @Test
    void successfulChargeIsStoredWithGatewayReference() {
        InMemoryHarness app = InMemoryHarness.create();
        Booking booking = heldBooking(app);
        Payment payment = app.payments.charge(booking, booking.totalAmount(), PaymentMethod.CARD, "4242424242424242");
        assertTrue(payment.succeeded());
        assertTrue(payment.gatewayRef().isPresent());
        assertEquals(1, app.payments.historyOf(booking.id()).size());
    }

    @Test
    void declinedChargeIsRecordedThenReportedAsPaymentFailed() {
        InMemoryHarness app = InMemoryHarness.create();
        Booking booking = heldBooking(app);
        assertThrows(PaymentFailedException.class,
                () -> app.payments.charge(booking, booking.totalAmount(), PaymentMethod.CARD, "tok-FAIL"));
        Payment stored = app.payments.historyOf(booking.id()).get(0);
        assertEquals(PaymentStatus.FAILED, stored.status());
        assertTrue(stored.failureReason().isPresent());
    }

    @Test
    void scriptedGatewayDeclinesOnTokenAndOnSuffix() {
        ScriptedPaymentGateway gateway = new ScriptedPaymentGateway();
        Money amount = Money.of("100");
        assertTrue(gateway.charge(amount, PaymentMethod.CARD, "4242424242424242").success());
        assertFalse(gateway.charge(amount, PaymentMethod.CARD, "my-fail-card").success());
        assertFalse(gateway.charge(amount, PaymentMethod.WALLET, "4111111111110000").success());
    }

    @Test
    void decliningGatewayAlwaysDeclinesWithItsReason() {
        GatewayReceipt receipt = new DecliningPaymentGateway("bank down")
                .charge(Money.of("10"), PaymentMethod.BANK_TRANSFER, "acct-9");
        assertFalse(receipt.success());
        assertEquals("bank down", receipt.failureReason());
        assertThrows(ValidationException.class, () -> new DecliningPaymentGateway(" "));
    }

    @Test
    void invalidChargeRequestsAreRejectedBeforeReachingTheGateway() {
        InMemoryHarness app = InMemoryHarness.create();
        Booking booking = heldBooking(app);
        Money total = booking.totalAmount();
        assertThrows(ValidationException.class,
                () -> app.payments.charge(booking, total.plus(Money.of("1")), PaymentMethod.CARD, "4242"));
        assertThrows(ValidationException.class,
                () -> app.payments.charge(booking, Money.ZERO, PaymentMethod.CARD, "4242"));
        assertThrows(ValidationException.class, () -> app.payments.charge(booking, total, null, "4242"));
        assertThrows(ValidationException.class, () -> app.payments.charge(booking, total, PaymentMethod.CARD, " "));
        assertThrows(ValidationException.class, () -> app.payments.charge(null, total, PaymentMethod.CARD, "4242"));
        assertTrue(app.payments.historyOf(booking.id()).isEmpty());
    }
}
