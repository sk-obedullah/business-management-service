package com.jewelry.repository.impl;

import com.jewelry.entity.Product;
import com.jewelry.repository.ProductRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link ProductRepository}.
 *
 * <p>
 * All SQL is parameterised — no string concatenation.
 * Row mapping is centralised in {@link #mapRow(ResultSet)}.
 *
 * <p>
 * Phase 1 stub: methods throw {@link UnsupportedOperationException}
 * as placeholders; they will be fully implemented in Phase 2.
 */
public class ProductRepositoryImpl extends AbstractJdbcRepository implements ProductRepository {

    private static final String INSERT_SQL = """
            INSERT INTO product
              (name, sku, category, metal, purity, weight_grams,
               cost_price, selling_price, quantity_on_hand, description,
               created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;

    private static final String SELECT_BY_ID = """
            SELECT * FROM product WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT * FROM product ORDER BY created_at DESC
            """;

    private static final String UPDATE_SQL = """
            UPDATE product SET
              name = ?, sku = ?, category = ?, metal = ?, purity = ?,
              weight_grams = ?, cost_price = ?, selling_price = ?,
              quantity_on_hand = ?, description = ?, updated_at = NOW()
            WHERE id = ?
            """;

    private static final String DELETE_BY_ID = "DELETE FROM product WHERE id = ?";
    private static final String EXISTS_BY_SKU = "SELECT COUNT(*) FROM product WHERE sku = ?";
    private static final String SELECT_BY_SKU = "SELECT * FROM product WHERE sku = ?";
    private static final String SELECT_LOW_STOCK = "SELECT * FROM product WHERE quantity_on_hand <= ? ORDER BY quantity_on_hand ASC";
    private static final String SELECT_BY_CATEGORY = "SELECT * FROM product WHERE LOWER(category) = LOWER(?) ORDER BY name ASC";
    private static final String SEARCH_SQL = "SELECT * FROM product WHERE name LIKE ? OR description LIKE ? ORDER BY name ASC";

    public ProductRepositoryImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Product save(Product p) {
        long id = executeInsert(INSERT_SQL,
                p.getName(), p.getSku(), p.getCategory(), p.getMetal(), p.getPurity(),
                p.getWeightGrams(), p.getCostPrice(), p.getSellingPrice(),
                p.getQuantityOnHand(), p.getDescription());
        p.setId(id);
        log.debug("Saved product id={} sku={}", id, p.getSku());
        return p;
    }

    @Override
    public Optional<Product> findById(Long id) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("findById failed for id=" + id, e);
        }
    }

    @Override
    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("findAll products failed", e);
        }
        return list;
    }

    @Override
    public void update(Product p) {
        executeUpdate(UPDATE_SQL,
                p.getName(), p.getSku(), p.getCategory(), p.getMetal(), p.getPurity(),
                p.getWeightGrams(), p.getCostPrice(), p.getSellingPrice(),
                p.getQuantityOnHand(), p.getDescription(), p.getId());
        log.debug("Updated product id={}", p.getId());
    }

    @Override
    public void deleteById(Long id) {
        int rows = executeUpdate(DELETE_BY_ID, id);
        log.debug("Deleted {} product row(s) for id={}", rows, id);
    }

    @Override
    public boolean existsBySku(String sku) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(EXISTS_BY_SKU)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("existsBySku failed for sku=" + sku, e);
        }
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_SKU)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("findBySku failed", e);
        }
    }

    @Override
    public List<Product> findLowStock(int threshold) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_LOW_STOCK)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("findLowStock failed", e);
        }
        return list;
    }

    @Override
    public List<Product> findByCategory(String category) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_CATEGORY)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("findByCategory failed", e);
        }
        return list;
    }

    @Override
    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SEARCH_SQL)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new com.jewelry.exception.ServiceException("search products failed", e);
        }
        return list;
    }

    // ── Row Mapper ───────────────────────────────────────────────────────────

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setSku(rs.getString("sku"));
        p.setCategory(rs.getString("category"));
        p.setMetal(rs.getString("metal"));
        p.setPurity(rs.getString("purity"));
        p.setWeightGrams(rs.getBigDecimal("weight_grams"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setQuantityOnHand(rs.getInt("quantity_on_hand"));
        p.setDescription(rs.getString("description"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null)
            p.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null)
            p.setUpdatedAt(updatedAt.toLocalDateTime());
        return p;
    }
}
