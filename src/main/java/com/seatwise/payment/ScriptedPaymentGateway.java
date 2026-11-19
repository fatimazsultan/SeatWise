package com.seatwise.payment;

import com.seatwise.common.Guard;
import com.seatwise.common.Ids;
import com.seatwise.common.Money;

/**
 * Succeeds unless the instrument contains {@code FAIL} or ends with {@code 0000}.
 * Those tokens are the decline path used in tests and the instructor demo.
 */
public final class ScriptedPaymentGateway implements PaymentGateway {

    public static final String DECLINE_TOKEN = "FAIL";
    public static final String DECLINE_SUFFIX = "0000";

    @Override
    public GatewayReceipt charge(Money amount, PaymentMethod method, String instrumentRef) {
        Guard.notNull(amount, "amount");
        Guard.that(!amount.isZero() && !amount.isNegative(), "amount must be positive");
        Guard.notNull(method, "method");
        Guard.notBlank(instrumentRef, "instrumentRef");
        if (shouldDecline(instrumentRef)) {
            return GatewayReceipt.declined("Gateway declined instrument " + instrumentRef);
        }
        return GatewayReceipt.ok(Ids.next("GW"));
    }

    private static boolean shouldDecline(String instrumentRef) {
        String token = instrumentRef.trim().toUpperCase();
        return token.contains(DECLINE_TOKEN) || token.endsWith(DECLINE_SUFFIX);
    }
}
