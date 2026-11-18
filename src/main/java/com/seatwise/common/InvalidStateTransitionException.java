package com.seatwise.common;

/** Thrown when an operation is not legal in the entity's current lifecycle state. */
public class InvalidStateTransitionException extends SeatWiseException {

    private static final long serialVersionUID = 1L;
    public static final String CODE = "INVALID_STATE_TRANSITION";

    public InvalidStateTransitionException(String entity, String currentState, String operation) {
        super(CODE, "Cannot " + operation + " " + entity + " while in state " + currentState);
    }
}
