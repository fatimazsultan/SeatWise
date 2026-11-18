package com.seatwise.booking;

import com.seatwise.common.Guard;
import java.util.Optional;

/** Person making a booking. Phone is optional; SMS is sent only when it is present. */
public record Customer(String name, String email, String phone) {

    private static final int MIN_PHONE_DIGITS = 7;
    private static final int MAX_PHONE_DIGITS = 15;

    public Customer {
        Guard.notBlank(name, "customer.name");
        email = Guard.email(email, "customer.email");
        phone = normalisePhone(phone);
    }

    public Customer(String name, String email) {
        this(name, email, null);
    }

    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phone);
    }

    private static String normalisePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        Guard.inRange(digits.length(), MIN_PHONE_DIGITS, MAX_PHONE_DIGITS, "customer.phone digit count");
        return phone.trim();
    }
}
