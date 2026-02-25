package com.jewelry.service;

import java.io.File;
import java.time.LocalDate;

/**
 * Contract for generating and exporting business reports as CSV files.
 *
 * <p>
 * Supported report types (each maps to a dedicated JDBC query):
 * <ul>
 * <li><b>ORDER_REPORT</b> — All orders with customer, status, total, net,
 * profit;
 * filterable by date range and status</li>
 * <li><b>REVENUE_SUMMARY</b> — Revenue and profit aggregated by month</li>
 * <li><b>INVENTORY_REPORT</b> — All products with cost, price, stock, and
 * margin</li>
 * <li><b>CUSTOMER_HISTORY</b> — Per-customer order count and spend</li>
 * </ul>
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> swap {@link File} targets for
 * {@code StreamingResponseBody} and stream directly to the HTTP response.
 */
public interface ReportService {

    enum ReportType {
        ORDER_REPORT("Order Report"),
        REVENUE_SUMMARY("Revenue Summary by Month"),
        INVENTORY_REPORT("Product Inventory"),
        CUSTOMER_HISTORY("Customer Purchase History");

        private final String displayName;

        ReportType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Generates a CSV file and writes it to {@code targetFile}.
     *
     * @param type         which report to generate
     * @param targetFile   destination file (created or overwritten)
     * @param from         inclusive start date filter (may be null → no lower
     *                     bound)
     * @param to           inclusive end date filter (may be null → no upper bound)
     * @param statusFilter optional order status filter (null or blank → all
     *                     statuses)
     * @return number of data rows written (excluding header)
     */
    int exportCsv(ReportType type, File targetFile,
            LocalDate from, LocalDate to,
            String statusFilter);
}
