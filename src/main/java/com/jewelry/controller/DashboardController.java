package com.jewelry.controller;

import com.jewelry.config.AppContext;
import com.jewelry.dto.DashboardSummary;
import com.jewelry.service.DashboardService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard screen (Dashboard.fxml).
 *
 * <p>
 * All data is fetched asynchronously via a background {@link Task} so
 * the JavaFX Application Thread is never blocked on database calls.
 * The UI is assembled purely in Java here (dynamically building KPI cards and
 * injecting chart data), which keeps the FXML lean and reusable.
 *
 * <p>
 * <strong>Architecture rule:</strong> No SQL or business logic here.
 * Only presentation: binding data to controls.
 */
public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML
    private FlowPane kpiPane;
    @FXML
    private BarChart<String, Number> revenueChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private PieChart statusPieChart;
    @FXML
    private TableView<DashboardSummary.TopProduct> topProductsTable;
    @FXML
    private TableColumn<DashboardSummary.TopProduct, String> colTopName;
    @FXML
    private TableColumn<DashboardSummary.TopProduct, String> colTopSku;
    @FXML
    private TableColumn<DashboardSummary.TopProduct, Long> colTopUnits;
    @FXML
    private TableColumn<DashboardSummary.TopProduct, BigDecimal> colTopRevenue;
    @FXML
    private ListView<String> lowStockList;
    @FXML
    private Label lblLastRefresh;
    @FXML
    private ProgressIndicator loadingIndicator;

    private final DashboardService dashboardService;

    public DashboardController() {
        this.dashboardService = AppContext.getInstance().getDashboardService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTopProductsTable();
        loadAsync();
    }

    // ── Async Load ────────────────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        loadAsync();
    }

    private void loadAsync() {
        loadingIndicator.setVisible(true);
        kpiPane.setDisable(true);

        Task<DashboardSummary> task = new Task<>() {
            @Override
            protected DashboardSummary call() {
                return dashboardService.getSummary();
            }
        };

        task.setOnSucceeded(e -> {
            DashboardSummary summary = task.getValue();
            populateKpiCards(summary);
            populateRevenueChart(summary);
            populateStatusPie(summary);
            populateTopProducts(summary);
            populateLowStock(summary);
            lblLastRefresh.setText("Last updated: " +
                    java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")));
            loadingIndicator.setVisible(false);
            kpiPane.setDisable(false);
            log.info("Dashboard refreshed");
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            kpiPane.setDisable(false);
            log.error("Dashboard load failed", task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── KPI Cards ─────────────────────────────────────────────────────────────

    private void populateKpiCards(DashboardSummary s) {
        kpiPane.getChildren().setAll(
                kpiCard("💰 Total Revenue", "₹ " + s.getTotalRevenue().toPlainString(), "#e2b04a"),
                kpiCard("📈 Gross Profit", "₹ " + s.getTotalProfit().toPlainString(), "#2ecc71"),
                kpiCard("% Profit Margin", s.getProfitMarginPct() + "%", "#3498db"),
                kpiCard("📦 Orders Today", String.valueOf(s.getOrdersToday()), "#9b59b6"),
                kpiCard("⏳ Pending Orders", String.valueOf(s.getPendingOrders()), "#f39c12"),
                kpiCard("👤 Customers", String.valueOf(s.getTotalCustomers()), "#1abc9c"),
                kpiCard("💎 Products", String.valueOf(s.getTotalProducts()), "#e67e22"),
                kpiCard("⚠ Low Stock", String.valueOf(s.getLowStockCount()), "#e74c3c"));
    }

    private VBox kpiCard(String title, String value, String accentColor) {
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        VBox card = new VBox(6, lblTitle, lblValue);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setPrefWidth(180);
        card.setStyle(
                "-fx-background-color: #16213e;" +
                        "-fx-border-color: " + accentColor + "44;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");
        return card;
    }

    // ── Revenue Bar Chart ─────────────────────────────────────────────────────

    private void populateRevenueChart(DashboardSummary s) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (₹)");
        if (s.getMonthlyRevenue().isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            s.getMonthlyRevenue().forEach((month, revenue) -> series.getData().add(new XYChart.Data<>(month, revenue)));
        }
        revenueChart.getData().add(series);
        revenueChart.setLegendVisible(false);

        // Style bars with gold colour after data is rendered
        revenueChart.lookupAll(".bar").forEach(node -> node.setStyle("-fx-bar-fill: #e2b04a;"));
    }

    // ── Status Pie Chart ──────────────────────────────────────────────────────

    private void populateStatusPie(DashboardSummary s) {
        statusPieChart.getData().clear();
        if (s.getOrdersByStatus().isEmpty()) {
            statusPieChart.getData().add(new PieChart.Data("No orders", 1));
        } else {
            s.getOrdersByStatus().forEach((status, count) -> statusPieChart.getData()
                    .add(new PieChart.Data(status + " (" + count + ")", count)));
        }
        statusPieChart.setLegendVisible(true);
        statusPieChart.setLabelsVisible(true);
    }

    // ── Top Products Table ────────────────────────────────────────────────────

    private void configureTopProductsTable() {
        colTopName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name()));
        colTopSku.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().sku()));
        colTopUnits.setCellValueFactory(
                data -> new javafx.beans.property.SimpleLongProperty(data.getValue().unitsSold()).asObject());
        colTopRevenue.setCellValueFactory(
                data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().revenue()));
        colTopRevenue.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "₹ " + v.toPlainString());
            }
        });
        topProductsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void populateTopProducts(DashboardSummary s) {
        if (s.getTopProducts() != null) {
            topProductsTable.getItems().setAll(s.getTopProducts());
        }
    }

    // ── Low Stock List ────────────────────────────────────────────────────────

    private void populateLowStock(DashboardSummary s) {
        lowStockList.getItems().clear();
        if (s.getLowStockProducts() == null || s.getLowStockProducts().isEmpty()) {
            lowStockList.getItems().add("✅  All products have sufficient stock.");
        } else {
            s.getLowStockProducts().forEach(p -> lowStockList.getItems().add(
                    "⚠  " + p.name() + " [" + p.sku() + "]  —  " + p.quantityOnHand() + " left"));
        }
    }
}
