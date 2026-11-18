package com.seatwise.common;

/** Thrown when a payment gateway declines or cannot process a charge. */
public class PaymentFailedException extends SeatWiseException {

    private static final long serialVersionUID = 1L;
    public static final String CODE = "PAYMENT_FAILED";

    public PaymentFailedException(String reason) {
        super(CODE, "Payment failed: " + reason);
    }
}
