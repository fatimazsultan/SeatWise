package com.seatwise.payment;

import com.seatwise.common.Money;

/**
 * Strategy for collecting money. The demo and tests inject a scripted gateway so
 * success and decline are deterministic without a real card network.
 */
public interface PaymentGateway {

    GatewayReceipt charge(Money amount, PaymentMethod method, String instrumentRef);
}
