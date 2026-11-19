package com.seatwise.common;

/**
 * Root of the SeatWise exception hierarchy. Every domain failure carries a stable
 * {@link #errorCode()} so the UI and tests can classify it without parsing messages.
 */
public class SeatWiseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public SeatWiseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SeatWiseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
