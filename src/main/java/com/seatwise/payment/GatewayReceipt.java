package com.seatwise.payment;

import com.seatwise.common.Guard;

/** What a {@link PaymentGateway} returns. The service turns this into a stored {@link Payment}. */
public record GatewayReceipt(boolean success, String reference, String failureReason) {

    public GatewayReceipt {
        if (success) {
            Guard.notBlank(reference, "reference");
            failureReason = null;
        } else {
            Guard.notBlank(failureReason, "failureReason");
        }
    }

    public static GatewayReceipt ok(String reference) {
        return new GatewayReceipt(true, reference, null);
    }

    public static GatewayReceipt declined(String reason) {
        return new GatewayReceipt(false, null, reason);
    }
}
