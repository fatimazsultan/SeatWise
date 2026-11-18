package com.seatwise.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Clock whose instant can be moved forward. Tests and the demo use it to expire holds. */
public final class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(Instant instant) {
        this(instant, ZoneOffset.UTC);
    }

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = Guard.notNull(instant, "instant");
        this.zone = Guard.notNull(zone, "zone");
    }

    public static MutableClock epoch() {
        return new MutableClock(Instant.parse("2026-06-01T09:00:00Z"));
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(Guard.notNull(duration, "duration"));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
