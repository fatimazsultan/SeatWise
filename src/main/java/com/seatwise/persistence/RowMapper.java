package com.seatwise.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps one JDBC row to a domain object. */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}

/** Binds parameters onto a prepared statement. */
@FunctionalInterface
interface Binder {
    void bind(PreparedStatement ps) throws SQLException;
}

/** Work that runs inside one database transaction. */
@FunctionalInterface
interface SqlWork<T> {
    T run() throws SQLException;
}
