package com.seatwise.common;

import java.util.UUID;

/** Prefixed unique ids. {@code UUID.randomUUID} is thread-safe. */
public final class Ids {

    private static final int SHORT_LENGTH = 8;

    private Ids() {
    }

    public static String next(String prefix) {
        Guard.notBlank(prefix, "prefix");
        String raw = UUID.randomUUID().toString().replace("-", "");
        return prefix + "-" + raw.substring(0, SHORT_LENGTH);
    }
}
