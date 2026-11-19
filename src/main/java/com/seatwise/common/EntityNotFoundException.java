package com.seatwise.common;

/** Thrown when a requested event, seat, booking or payment does not exist. */
public class EntityNotFoundException extends SeatWiseException {

    private static final long serialVersionUID = 1L;
    public static final String CODE = "NOT_FOUND";

    public EntityNotFoundException(String entityType, String id) {
        super(CODE, entityType + " with id '" + id + "' was not found");
    }
}
