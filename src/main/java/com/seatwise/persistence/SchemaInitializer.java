package com.seatwise.persistence;

import com.seatwise.common.Guard;
import com.seatwise.common.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Applies schema.sql. Statements use IF NOT EXISTS, so running twice is safe. */
public final class SchemaInitializer {

    static final String SCHEMA_RESOURCE = "/schema.sql";

    private SchemaInitializer() {
    }

    public static void apply(Database database) {
        Guard.notNull(database, "database");
        for (String statement : splitStatements(readSchema())) {
            database.execute(statement);
        }
    }

    static String readSchema() {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new PersistenceException("Classpath resource " + SCHEMA_RESOURCE + " is missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("Failed to read " + SCHEMA_RESOURCE, e);
        }
    }

    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\\R")) {
            appendLine(statements, current, line.trim());
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString().strip());
        }
        return statements;
    }

    private static void appendLine(List<String> statements, StringBuilder current, String trimmed) {
        if (trimmed.isEmpty() || trimmed.startsWith("--")) {
            return;
        }
        current.append(trimmed).append('\n');
        if (trimmed.endsWith(";")) {
            statements.add(current.toString().strip());
            current.setLength(0);
        }
    }
}
