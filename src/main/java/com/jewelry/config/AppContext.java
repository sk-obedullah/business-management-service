package com.jewelry.config;

import com.jewelry.repository.CustomerRepository;
import com.jewelry.repository.OrderLineRepository;
import com.jewelry.repository.OrderRepository;
import com.jewelry.repository.ProductRepository;
import com.jewelry.repository.impl.CustomerRepositoryImpl;
import com.jewelry.repository.impl.OrderLineRepositoryImpl;
import com.jewelry.repository.impl.OrderRepositoryImpl;
import com.jewelry.repository.impl.ProductRepositoryImpl;
import com.jewelry.service.CustomerService;
import com.jewelry.service.DashboardService;
import com.jewelry.service.OrderService;
import com.jewelry.service.ProductService;
import com.jewelry.service.ReportService;
import com.jewelry.service.impl.CustomerServiceImpl;
import com.jewelry.service.impl.DashboardServiceImpl;
import com.jewelry.service.impl.OrderServiceImpl;
import com.jewelry.service.impl.ProductServiceImpl;
import com.jewelry.service.impl.ReportServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Manual Dependency-Injection container.
 *
 * <p>
 * All repositories and services are wired here and exposed as singletons.
 * Controllers retrieve their collaborators from this context via static
 * accessors.
 *
 * <p>
 * <strong>Why manual DI instead of a framework?</strong><br>
 * This is a standalone desktop app with no class-scanning runtime. A full DI
 * framework would add complexity without benefit at this scale. The trade-off
 * is explicit wiring; the benefit is complete transparency and zero magic.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> Delete this class entirely.
 * Annotate repositories with {@code @Repository}, services with
 * {@code @Service},
 * and inject via constructor {@code @Autowired} or constructor injection.
 */
public final class AppContext {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    // ── Singleton instance ──────────────────────────────────────────────────
    private static volatile AppContext instance;

    // ── Infrastructure ──────────────────────────────────────────────────────
    private final DataSource dataSource;

    // ── Repositories ───────────────────────────────────────────────────────
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;

    // ── Services ───────────────────────────────────────────────────────────
    private final ProductService productService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final DashboardService dashboardService;
    private final ReportService reportService;

    // ── Constructor: wire everything ────────────────────────────────────────
    private AppContext() {
        log.info("Assembling application context...");

        this.dataSource = DatabaseConfig.getDataSource();

        // Repositories
        this.productRepository = new ProductRepositoryImpl(dataSource);
        this.customerRepository = new CustomerRepositoryImpl(dataSource);
        this.orderRepository = new OrderRepositoryImpl(dataSource);
        this.orderLineRepository = new OrderLineRepositoryImpl(dataSource);

        // Services
        this.productService = new ProductServiceImpl(productRepository);
        this.customerService = new CustomerServiceImpl(customerRepository);
        this.orderService = new OrderServiceImpl(
                orderRepository, orderLineRepository, productRepository, dataSource);
        this.dashboardService = new DashboardServiceImpl(dataSource);
        this.reportService = new ReportServiceImpl(dataSource);

        log.info("Application context assembled successfully.");
    }

    /** Returns the singleton {@link AppContext}, initializing it on first call. */
    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    // ── Public accessors ────────────────────────────────────────────────────

    public DataSource getDataSource() {
        return dataSource;
    }

    public ProductService getProductService() {
        return productService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public ProductRepository getProductRepository() {
        return productRepository;
    }
}
