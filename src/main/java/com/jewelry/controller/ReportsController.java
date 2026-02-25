package com.jewelry.controller;

import com.jewelry.config.AppContext;
import com.jewelry.service.ReportService;
import com.jewelry.service.ReportService.ReportType;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the Reports screen (Reports.fxml).
 *
 * <p>
 * Allows the user to:
 * <ol>
 * <li>Pick a report type (ComboBox)</li>
 * <li>Set an optional date range via DatePickers</li>
 * <li>Optionally filter by order status (for ORDER_REPORT)</li>
 * <li>Click Export → JavaFX FileChooser opens → CSV written asynchronously</li>
 * </ol>
 *
 * <p>
 * Export runs on a daemon background thread (Task) so the UI never freezes
 * during large result-set serialisation.
 */
public class ReportsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML
    private ComboBox<ReportType> cboReportType;
    @FXML
    private DatePicker dpFrom;
    @FXML
    private DatePicker dpTo;
    @FXML
    private ComboBox<String> cboStatus;
    @FXML
    private Label lblStatusFilter;
    @FXML
    private Button btnExport;
    @FXML
    private Label lblResult;
    @FXML
    private ProgressIndicator loadingIndicator;

    // ── Preview table ─────────────────────────────────────────────────────────
    @FXML
    private ListView<String> previewList;

    private final ReportService reportService;

    public ReportsController() {
        this.reportService = AppContext.getInstance().getReportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Report type picker
        cboReportType.getItems().setAll(ReportType.values());
        cboReportType.setValue(ReportType.ORDER_REPORT);

        // Status filter (only applies to ORDER_REPORT)
        cboStatus.getItems().addAll("All", "PENDING", "PROCESSING", "COMPLETED", "CANCELLED");
        cboStatus.setValue("All");

        // Show/hide status filter depending on selected report type
        cboReportType.valueProperty().addListener((obs, old, type) -> {
            boolean showStatus = type == ReportType.ORDER_REPORT;
            cboStatus.setVisible(showStatus);
            lblStatusFilter.setVisible(showStatus);
            updatePreview();
        });

        dpFrom.valueProperty().addListener((obs, o, n) -> updatePreview());
        dpTo.valueProperty().addListener((obs, o, n) -> updatePreview());
        cboStatus.valueProperty().addListener((obs, o, n) -> updatePreview());

        lblResult.setText("");
        loadingIndicator.setVisible(false);
        updatePreview();
    }

    @FXML
    private void onClearDates() {
        dpFrom.setValue(null);
        dpTo.setValue(null);
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    private void updatePreview() {
        ReportType type = cboReportType.getValue();
        previewList.getItems().setAll(
                "📊  Report:  " + (type != null ? type.getDisplayName() : "—"),
                "📅  From:    " + (dpFrom.getValue() != null ? dpFrom.getValue().toString() : "No lower bound"),
                "📅  To:      " + (dpTo.getValue() != null ? dpTo.getValue().toString() : "No upper bound"),
                (type == ReportType.ORDER_REPORT
                        ? "🏷  Status:  " + cboStatus.getValue()
                        : ""),
                "",
                "Columns exported:",
                getColumns(type));
    }

    private String getColumns(ReportType type) {
        if (type == null)
            return "—";
        return switch (type) {
            case ORDER_REPORT ->
                "  Order ID | Customer | Email | Date | Status | Total | Discount | Net | Profit | Items | Notes";
            case REVENUE_SUMMARY -> "  Month | Orders | Revenue | Profit | Margin %";
            case INVENTORY_REPORT ->
                "  Product | SKU | Category | Material | Cost | Price | Unit Profit | Margin % | Stock | Stock Value";
            case CUSTOMER_HISTORY -> "  Customer | Email | Phone | Total Orders | Total Spend | Last Order Date";
        };
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @FXML
    private void onExport() {
        ReportType type = cboReportType.getValue();
        if (type == null) {
            showResult(false, "Please select a report type.");
            return;
        }

        // Validate date range
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        if (from != null && to != null && from.isAfter(to)) {
            showResult(false, "'From' date must not be after 'To' date.");
            return;
        }

        // FileChooser
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report as CSV");
        chooser.setInitialFileName(
                type.name().toLowerCase() + "_" + LocalDate.now().format(FILE_DATE_FMT) + ".csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Stage stage = (Stage) btnExport.getScene().getWindow();
        File target = chooser.showSaveDialog(stage);
        if (target == null)
            return; // user cancelled

        String statusFilter = cboStatus.getValue();
        doExportAsync(type, target, from, to, statusFilter);
    }

    private void doExportAsync(ReportType type, File target,
            LocalDate from, LocalDate to, String statusFilter) {
        btnExport.setDisable(true);
        loadingIndicator.setVisible(true);
        lblResult.setText("Exporting…");

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return reportService.exportCsv(type, target, from, to, statusFilter);
            }
        };

        task.setOnSucceeded(e -> {
            btnExport.setDisable(false);
            loadingIndicator.setVisible(false);
            int rows = task.getValue();
            showResult(true, "✔  Exported " + rows + " rows to: " + target.getName());
            log.info("Report exported: type={} rows={} file={}", type, rows, target.getAbsolutePath());
        });

        task.setOnFailed(e -> {
            btnExport.setDisable(false);
            loadingIndicator.setVisible(false);
            String msg = task.getException() != null
                    ? task.getException().getMessage()
                    : "Unknown error";
            showResult(false, "✘  Export failed: " + msg);
            log.error("Report export failed", task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showResult(boolean success, String msg) {
        lblResult.setText(msg);
        lblResult.setStyle(success
                ? "-fx-text-fill: #2ecc71; -fx-font-weight: bold;"
                : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }
}
