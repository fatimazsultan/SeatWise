package com.seatwise.common;

/** Thrown when input fails a precondition check at a module boundary. */
public class ValidationException extends SeatWiseException {

    private static final long serialVersionUID = 1L;
    public static final String CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(CODE, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
