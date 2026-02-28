package com.jewelry.controller;

import com.jewelry.dto.DashboardSummary;
import com.jewelry.service.DashboardService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard screen (Dashboard.fxml).
 *
 * <p>
 * All data is fetched asynchronously via a background {@link Task} so
 * the JavaFX Application Thread is never blocked on database calls.
 */
@Component
public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML
    private TilePane kpiPane;
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
                kpiCard("💰 Total Revenue", "₹ " + s.totalRevenue().toPlainString(), "#e2b04a"),
                kpiCard("📈 Gross Profit", "₹ " + s.totalProfit().toPlainString(), "#2ecc71"),
                kpiCard("% Profit Margin", s.profitMarginPct() + "%", "#3498db"),
                kpiCard("📦 Orders Today", String.valueOf(s.ordersToday()), "#9b59b6"),
                kpiCard("⏳ Pending Orders", String.valueOf(s.pendingOrders()), "#f39c12"),
                kpiCard("👤 Customers", String.valueOf(s.totalCustomers()), "#1abc9c"),
                kpiCard("💎 Products", String.valueOf(s.totalProducts()), "#e67e22"),
                kpiCard("⚠️ Low Stock", String.valueOf(s.lowStockCount()), "#e74c3c"));
    }

    private VBox kpiCard(String title, String value, String accentColor) {
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 26px; -fx-font-weight: bold;");

        VBox card = new VBox(8, lblTitle, lblValue);
        card.getStyleClass().add("kpi-card");

        // Dynamic tile sizing (TilePane will use these)
        card.setPrefWidth(240);
        card.setMinHeight(100);

        return card;
    }

    // ── Revenue Bar Chart ─────────────────────────────────────────────────────

    private void populateRevenueChart(DashboardSummary s) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (₹)");
        if (s.monthlyRevenue().isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            s.monthlyRevenue().forEach((month, revenue) -> series.getData().add(new XYChart.Data<>(month, revenue)));
        }
        revenueChart.getData().add(series);
        revenueChart.setLegendVisible(false);

        // Style bars with gold colour after data is rendered
        revenueChart.lookupAll(".bar").forEach(node -> node.setStyle("-fx-bar-fill: #e2b04a;"));
    }

    // ── Status Pie Chart ──────────────────────────────────────────────────────

    private void populateStatusPie(DashboardSummary s) {
        statusPieChart.getData().clear();
        if (s.ordersByStatus().isEmpty()) {
            statusPieChart.getData().add(new PieChart.Data("No orders", 1));
        } else {
            s.ordersByStatus().forEach((status, count) -> statusPieChart.getData()
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

        // Make columns proportional so they stretch to fill the table width
        colTopName.prefWidthProperty().bind(topProductsTable.widthProperty().multiply(0.40));
        colTopSku.prefWidthProperty().bind(topProductsTable.widthProperty().multiply(0.25));
        colTopUnits.prefWidthProperty().bind(topProductsTable.widthProperty().multiply(0.15));
        colTopRevenue.prefWidthProperty().bind(topProductsTable.widthProperty().multiply(0.20));

        topProductsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void populateTopProducts(DashboardSummary s) {
        if (s.topProducts() != null) {
            topProductsTable.getItems().setAll(s.topProducts());
        }
    }

    // ── Low Stock List ────────────────────────────────────────────────────────

    private void populateLowStock(DashboardSummary s) {
        lowStockList.getItems().clear();
        if (s.lowStockProducts() == null || s.lowStockProducts().isEmpty()) {
            lowStockList.getItems().add("✅  All products have sufficient stock.");
        } else {
            s.lowStockProducts().forEach(p -> lowStockList.getItems().add(
                    "⚠️  " + p.name() + " [" + p.sku() + "]  —  " + p.quantityOnHand() + " left"));
        }
    }
}
