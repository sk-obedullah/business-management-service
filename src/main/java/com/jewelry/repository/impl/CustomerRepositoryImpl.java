package com.jewelry.repository.impl;

import com.jewelry.entity.Customer;
import com.jewelry.exception.ServiceException;
import com.jewelry.repository.CustomerRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link CustomerRepository}.
 */
public class CustomerRepositoryImpl extends AbstractJdbcRepository implements CustomerRepository {

    private static final String INSERT_SQL = """
            INSERT INTO customer
              (first_name, last_name, email, phone, address, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;

    private static final String SELECT_BY_ID = "SELECT * FROM customer WHERE id = ?";
    private static final String SELECT_ALL = "SELECT * FROM customer ORDER BY last_name, first_name";
    private static final String UPDATE_SQL = """
            UPDATE customer SET
              first_name = ?, last_name = ?, email = ?, phone = ?,
              address = ?, notes = ?, updated_at = NOW()
            WHERE id = ?
            """;
    private static final String DELETE_BY_ID = "DELETE FROM customer WHERE id = ?";
    private static final String SELECT_BY_EMAIL = "SELECT * FROM customer WHERE email = ?";
    private static final String EXISTS_BY_EMAIL = "SELECT COUNT(*) FROM customer WHERE email = ?";
    private static final String EXISTS_BY_PHONE = "SELECT COUNT(*) FROM customer WHERE phone = ?";
    private static final String SEARCH_SQL = """
            SELECT * FROM customer
            WHERE first_name LIKE ? OR last_name LIKE ? OR email LIKE ? OR phone LIKE ?
            ORDER BY last_name, first_name
            """;

    public CustomerRepositoryImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Customer save(Customer c) {
        long id = executeInsert(INSERT_SQL,
                c.getFirstName(), c.getLastName(), c.getEmail(),
                c.getPhone(), c.getAddress(), c.getNotes());
        c.setId(id);
        log.debug("Saved customer id={}", id);
        return c;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ServiceException("findById customer failed for id=" + id, e);
        }
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new ServiceException("findAll customers failed", e);
        }
        return list;
    }

    @Override
    public void update(Customer c) {
        executeUpdate(UPDATE_SQL,
                c.getFirstName(), c.getLastName(), c.getEmail(),
                c.getPhone(), c.getAddress(), c.getNotes(), c.getId());
    }

    @Override
    public void deleteById(Long id) {
        executeUpdate(DELETE_BY_ID, id);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ServiceException("findByEmail failed", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(EXISTS_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new ServiceException("existsByEmail failed", e);
        }
    }

    @Override
    public boolean existsByPhone(String phone) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(EXISTS_BY_PHONE)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new ServiceException("existsByPhone failed", e);
        }
    }

    @Override
    public List<Customer> search(String keyword) {
        String p = "%" + keyword + "%";
        List<Customer> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SEARCH_SQL)) {
            ps.setString(1, p);
            ps.setString(2, p);
            ps.setString(3, p);
            ps.setString(4, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new ServiceException("search customers failed", e);
        }
        return list;
    }

    // ── Row Mapper ───────────────────────────────────────────────────────────

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setNotes(rs.getString("notes"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null)
            c.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null)
            c.setUpdatedAt(updatedAt.toLocalDateTime());
        return c;
    }
}
