package com.seatwise.payment;

import com.seatwise.common.Guard;
import com.seatwise.common.Money;

/** Gateway that always declines. Used to inject a payment-failure scenario. */
public final class DecliningPaymentGateway implements PaymentGateway {

    private final String reason;

    public DecliningPaymentGateway(String reason) {
        this.reason = Guard.notBlank(reason, "reason");
    }

    @Override
    public GatewayReceipt charge(Money amount, PaymentMethod method, String instrumentRef) {
        Guard.notNull(amount, "amount");
        Guard.notNull(method, "method");
        Guard.notBlank(instrumentRef, "instrumentRef");
        return GatewayReceipt.declined(reason);
    }
}
