package com.seatwise.common;

/** Wraps JDBC/SQL failures so callers never depend on java.sql. */
public class PersistenceException extends SeatWiseException {

    private static final long serialVersionUID = 1L;
    public static final String CODE = "PERSISTENCE_ERROR";

    public PersistenceException(String message) {
        super(CODE, message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
