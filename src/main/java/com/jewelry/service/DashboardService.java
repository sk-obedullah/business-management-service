package com.jewelry.service;

import com.jewelry.dto.DashboardSummary;

/**
 * Contract for computing dashboard aggregates.
 *
 * <p>
 * All queries are read-only (no mutations).
 * The implementation fires several focused JDBC queries and assembles
 * the result into a single {@link DashboardSummary}.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> annotate the impl
 * with {@code @Service @Transactional(readOnly=true)}.
 */
public interface DashboardService {

    /**
     * Fetches all aggregated dashboard data in one call.
     * Safe to call from a background JavaFX {@code Task}.
     */
    DashboardSummary getSummary();
}
