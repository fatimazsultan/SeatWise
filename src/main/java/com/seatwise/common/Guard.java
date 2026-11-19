package com.seatwise.common;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Defensive checks applied at every public module boundary. Invalid input becomes a
 * {@link ValidationException} instead of a later null or corrupt row.
 */
public final class Guard {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Guard() {
    }

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new ValidationException(name + " must not be null");
        }
        return value;
    }

    public static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(name + " must not be blank");
        }
        return value;
    }

    public static <C extends Collection<?>> C notEmpty(C value, String name) {
        notNull(value, name);
        if (value.isEmpty()) {
            throw new ValidationException(name + " must not be empty");
        }
        return value;
    }

    public static int positive(int value, String name) {
        if (value <= 0) {
            throw new ValidationException(name + " must be positive but was " + value);
        }
        return value;
    }

    public static int inRange(int value, int minInclusive, int maxInclusive, String name) {
        if (value < minInclusive || value > maxInclusive) {
            throw new ValidationException(
                    name + " must be between " + minInclusive + " and " + maxInclusive + " but was " + value);
        }
        return value;
    }

    public static Money notNegative(Money value, String name) {
        notNull(value, name);
        if (value.isNegative()) {
            throw new ValidationException(name + " must not be negative but was " + value);
        }
        return value;
    }

    public static String email(String value, String name) {
        notBlank(value, name);
        if (!EMAIL.matcher(value.trim()).matches()) {
            throw new ValidationException(name + " is not a valid e-mail address: '" + value + "'");
        }
        return value.trim();
    }

    public static void that(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}
