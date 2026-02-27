package com.jewelry.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Central Spring configuration class.
 *
 * <p>Replaces the old {@code DatabaseConfig}, {@code AppContext}, and
 * {@code PropertiesLoader} with a single, clean {@code @Configuration} class.
 *
 * <p>Beans provided:
 * <ul>
 *   <li>{@link DataSource} — embedded H2, file-based</li>
 *   <li>{@link LocalContainerEntityManagerFactoryBean} — Hibernate as JPA provider</li>
 *   <li>{@link PlatformTransactionManager} — declarative {@code @Transactional} support</li>
 * </ul>
 */
@Configuration
@ComponentScan("com.jewelry")
@EnableJpaRepositories("com.jewelry.repository")
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
public class PersistenceConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password:}")
    private String dbPassword;

    // ── DataSource ────────────────────────────────────────────────────────────

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        return ds;
    }

    // ── EntityManagerFactory (Hibernate) ─────────────────────────────────────

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.jewelry.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties());
        return em;
    }

    // ── TransactionManager ────────────────────────────────────────────────────

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    // ── Hibernate Properties ──────────────────────────────────────────────────

    private Properties hibernateProperties() {
        Properties props = new Properties();
        // Auto-create tables; switch to 'validate' once schema is stable
        props.setProperty("hibernate.hbm2ddl.auto",           "update");
        props.setProperty("hibernate.dialect",                 "org.hibernate.dialect.H2Dialect");
        props.setProperty("hibernate.show_sql",                "false");
        props.setProperty("hibernate.format_sql",              "true");
        // H2 MODE=MySQL uses double-quote identifiers; this tells Hibernate to do the same
        props.setProperty("hibernate.globally_quoted_identifiers", "false");
        // Physical naming: camelCase fields → snake_case columns (weightGrams → weight_grams)
        props.setProperty("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        return props;
    }
}
