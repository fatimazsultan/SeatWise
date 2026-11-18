package com.seatwise.persistence;

import com.seatwise.common.Guard;
import com.seatwise.common.PersistenceException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One SQLite connection. An application lock stops two booking threads from
 * interleaving half-written transactions on that connection.
 */
public final class Database implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private final Connection connection;
    private final ReentrantLock lock = new ReentrantLock();

    public Database(String jdbcUrl) {
        Guard.notBlank(jdbcUrl, "jdbcUrl");
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
            this.connection.setAutoCommit(true);
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to open database " + jdbcUrl, e);
        }
    }

    public Connection connection() {
        return connection;
    }

    public void execute(String sql) {
        execute(sql, Database::bindNone);
    }

    public void execute(String sql, Binder binder) {
        lock.lock();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("SQL execute failed: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper) {
        return query(sql, mapper, Database::bindNone);
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Binder binder) {
        lock.lock();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            return readAll(ps, mapper);
        } catch (SQLException e) {
            throw new PersistenceException("SQL query failed: " + sql, e);
        } finally {
            lock.unlock();
        }
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Binder binder) {
        List<T> rows = query(sql, mapper, binder);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public <T> T inTransaction(SqlWork<T> work) {
        lock.lock();
        try {
            connection.setAutoCommit(false);
            T result = work.run();
            connection.commit();
            return result;
        } catch (SQLException e) {
            rollback();
            throw new PersistenceException("Transaction failed", e);
        } catch (RuntimeException e) {
            rollback();
            throw e;
        } finally {
            restoreAutoCommit();
            lock.unlock();
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to close database", e);
        }
    }

    private static <T> List<T> readAll(PreparedStatement ps, RowMapper<T> mapper) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<T> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapper.map(rs));
            }
            return rows;
        }
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Rollback failed", e);
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Could not restore autocommit", e);
        }
    }

    private static void bindNone(PreparedStatement ps) throws SQLException {
        Guard.notNull(ps, "ps");
    }
}
