package com.seatwise.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable money value with a fixed 2-decimal scale. Replaces {@code double} so pricing
 * cannot suffer floating-point rounding, and immutability makes it safe to share across threads.
 */
public final class Money implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Zero amount. Initialised after the scale constants so the constructor can round it. */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(String amount) {
        Guard.notBlank(amount, "amount");
        try {
            return new Money(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid money amount: '" + amount + "'", e);
        }
    }

    public static Money of(BigDecimal amount) {
        Guard.notNull(amount, "amount");
        return new Money(amount);
    }

    public static Money of(long wholeUnits) {
        return new Money(BigDecimal.valueOf(wholeUnits));
    }

    public Money plus(Money other) {
        Guard.notNull(other, "other");
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        Guard.notNull(other, "other");
        return new Money(amount.subtract(other.amount));
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money percent(int percentage) {
        Guard.inRange(percentage, 0, 100, "percentage");
        return new Money(amount.multiply(BigDecimal.valueOf(percentage)).divide(HUNDRED, SCALE, ROUNDING));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public String toPlainString() {
        return amount.toPlainString();
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "PKR " + amount.toPlainString();
    }
}
