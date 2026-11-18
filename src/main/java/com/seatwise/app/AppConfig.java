package com.seatwise.app;

import com.seatwise.common.Guard;
import com.seatwise.payment.PaymentGateway;
import com.seatwise.payment.ScriptedPaymentGateway;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

/** Settings for {@link SeatWiseApp}. */
public record AppConfig(Path databaseFile, Clock clock, Duration holdDuration, Duration expiryInterval,
        PaymentGateway paymentGateway, boolean seedIfEmpty) {

    public static final Duration DEFAULT_HOLD = Duration.ofMinutes(15);

    public AppConfig {
        Guard.notNull(databaseFile, "databaseFile");
        Guard.notNull(clock, "clock");
        Guard.notNull(holdDuration, "holdDuration");
        Guard.notNull(expiryInterval, "expiryInterval");
        Guard.notNull(paymentGateway, "paymentGateway");
        Guard.that(!holdDuration.isZero() && !holdDuration.isNegative(), "holdDuration must be positive");
        Guard.that(!expiryInterval.isZero() && !expiryInterval.isNegative(), "expiryInterval must be positive");
    }

    public static AppConfig interactive(Path databaseFile) {
        return new AppConfig(databaseFile, Clock.systemUTC(), DEFAULT_HOLD, Duration.ofSeconds(30),
                new ScriptedPaymentGateway(), true);
    }

    public static AppConfig demo(Path databaseFile, Clock clock, Duration holdDuration) {
        return new AppConfig(databaseFile, clock, holdDuration, Duration.ofSeconds(1),
                new ScriptedPaymentGateway(), true);
    }
}
