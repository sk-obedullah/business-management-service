package com.jewelry.repository.impl;

import com.jewelry.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;

/**
 * Abstract base class for all JDBC-based repositories.
 *
 * <p>
 * Centralises connection acquisition, statement execution, transaction helpers,
 * and error normalisation. Concrete repositories extend this and implement only
 * entity-specific row mapping and SQL strings.
 *
 * <p>
 * <strong>Architectural rule:</strong> No concrete repository should call
 * {@code DriverManager} or interact with {@link DataSource} directly —
 * all of that is encapsulated here.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> replace contents of concrete
 * {@code save/update/delete} methods with Spring Data JPA calls; this base
 * class
 * can be deleted and concrete repos can extend {@code JpaRepository}.
 */
public abstract class AbstractJdbcRepository {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final DataSource dataSource;

    protected AbstractJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Connection helper ────────────────────────────────────────────────────

    /**
     * Obtains a connection from the HikariCP pool.
     * The caller is responsible for closing it (preferably via try-with-resources).
     *
     * @throws ServiceException if the pool cannot supply a connection
     */
    protected Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new ServiceException("Failed to acquire DB connection from pool", e);
        }
    }

    // ── DML helper ───────────────────────────────────────────────────────────

    /**
     * Executes an INSERT and returns the generated primary key.
     *
     * @param sql    parameterised INSERT statement
     * @param params positional parameters in order
     * @return generated {@code BIGINT} key
     * @throws ServiceException on SQL error
     */
    protected long executeInsert(String sql, Object... params) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindParams(ps, params);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new ServiceException("INSERT produced no rows: " + sql);
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new ServiceException("INSERT succeeded but no generated key returned");
            }
        } catch (SQLException e) {
            throw new ServiceException("INSERT failed: " + sql, e);
        }
    }

    /**
     * Executes an UPDATE or DELETE statement.
     *
     * @param sql    parameterised statement
     * @param params positional parameters
     * @return number of rows affected
     */
    protected int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new ServiceException("UPDATE/DELETE failed: " + sql, e);
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Binds positional parameters to a prepared statement.
     * Handles {@code null} correctly via {@link PreparedStatement#setNull}.
     */
    private void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, Types.NULL);
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * Retrieves {@code Long} from a ResultSet column, returning {@code null}
     * if the column value was SQL NULL (avoids NPE from {@code getLong}).
     */
    protected Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long val = rs.getLong(column);
        return rs.wasNull() ? null : val;
    }
}
