-- =====================================================================
-- jewelry_db.sql  — Database Bootstrap Script
-- Run once before launching the application:
--   mysql -u root -p < src/main/resources/ddl/jewelry_db.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS jewelry_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jewelry_db;

-- ╔══════════╗
-- ║ PRODUCT  ║
-- ╚══════════╝
CREATE TABLE IF NOT EXISTS product (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    name             VARCHAR(255)   NOT NULL,
    sku              VARCHAR(100)   NOT NULL UNIQUE,
    category         VARCHAR(100)   NOT NULL COMMENT 'Ring, Necklace, Bracelet, Earring, etc.',
    metal            VARCHAR(100)   NOT NULL COMMENT 'Gold, Silver, Platinum, etc.',
    purity           VARCHAR(50)             COMMENT '18K, 22K, 925, etc.',
    weight_grams     DECIMAL(10,3)           COMMENT 'Weight in grams',
    cost_price       DECIMAL(15,2)  NOT NULL COMMENT 'Manufacturing / purchase cost',
    selling_price    DECIMAL(15,2)  NOT NULL COMMENT 'Listed retail price',
    quantity_on_hand INT            NOT NULL DEFAULT 0,
    description      TEXT,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_product_category (category),
    INDEX idx_product_metal     (metal),
    INDEX idx_product_qty       (quantity_on_hand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ╔══════════╗
-- ║ CUSTOMER ║
-- ╚══════════╝
CREATE TABLE IF NOT EXISTS customer (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255)          UNIQUE,
    phone      VARCHAR(30),
    address    TEXT,
    notes      TEXT,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_customer_email (email),
    INDEX idx_customer_name  (last_name, first_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ╔═══════╗
-- ║ ORDER ║
-- ╚═══════╝
CREATE TABLE IF NOT EXISTS `order` (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id    BIGINT        NOT NULL,
    order_date     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         ENUM('PENDING','PROCESSING','SHIPPED','IN_TRANSIT','DELIVERED','COMPLETED','CANCELLED')
                                 NOT NULL DEFAULT 'PENDING',
    total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount       DECIMAL(15,2)          DEFAULT 0.00,
    notes          TEXT,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
        REFERENCES customer(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_order_status      (status),
    INDEX idx_order_customer    (customer_id),
    INDEX idx_order_date        (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ╔════════════╗
-- ║ ORDER_LINE ║
-- ╚════════════╝
CREATE TABLE IF NOT EXISTS order_line (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    unit_price   DECIMAL(15,2) NOT NULL COMMENT 'Price at time of sale (snapshot)',
    cost_price   DECIMAL(15,2) NOT NULL COMMENT 'Cost at time of sale (for profit calc)',

    PRIMARY KEY (id),
    CONSTRAINT fk_line_order   FOREIGN KEY (order_id)   REFERENCES `order`(id)   ON DELETE CASCADE,
    CONSTRAINT fk_line_product FOREIGN KEY (product_id) REFERENCES product(id)   ON DELETE RESTRICT,
    INDEX idx_line_order    (order_id),
    INDEX idx_line_product  (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
