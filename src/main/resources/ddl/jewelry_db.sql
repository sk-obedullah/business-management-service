-- =====================================================================
-- jewelry_db.sql  — H2 Embedded Database Schema
-- This script is executed automatically on the first launch.
-- No manual setup required.
-- =====================================================================

-- ╔══════════╗
-- ║ PRODUCT  ║
-- ╚══════════╝
CREATE TABLE IF NOT EXISTS product (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    name             VARCHAR(255)   NOT NULL,
    sku              VARCHAR(100)   NOT NULL UNIQUE,
    category         VARCHAR(100)   NOT NULL,
    metal            VARCHAR(100)   NOT NULL,
    purity           VARCHAR(50),
    weight_grams     DECIMAL(10,3),
    cost_price       DECIMAL(15,2)  NOT NULL,
    selling_price    DECIMAL(15,2)  NOT NULL,
    quantity_on_hand INT            NOT NULL DEFAULT 0,
    description      TEXT,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_product_category ON product (category);
CREATE INDEX IF NOT EXISTS idx_product_metal     ON product (metal);
CREATE INDEX IF NOT EXISTS idx_product_qty       ON product (quantity_on_hand);

-- ╔══════════╗
-- ║ CUSTOMER ║
-- ╚══════════╝
CREATE TABLE IF NOT EXISTS customer (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) UNIQUE,
    phone      VARCHAR(30),
    address    TEXT,
    notes      TEXT,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_customer_email ON customer (email);
CREATE INDEX IF NOT EXISTS idx_customer_name  ON customer (last_name, first_name);

-- ╔═══════╗
-- ║ ORDER ║
-- ╚═══════╝
CREATE TABLE IF NOT EXISTS "order" (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id    BIGINT        NOT NULL,
    order_date     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount       DECIMAL(15,2)          DEFAULT 0.00,
    notes          TEXT,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
        REFERENCES customer(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_status   ON "order" (status);
CREATE INDEX IF NOT EXISTS idx_order_customer ON "order" (customer_id);
CREATE INDEX IF NOT EXISTS idx_order_date     ON "order" (order_date);

-- ╔════════════╗
-- ║ ORDER_LINE ║
-- ╚════════════╝
CREATE TABLE IF NOT EXISTS order_line (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    unit_price   DECIMAL(15,2) NOT NULL,
    cost_price   DECIMAL(15,2) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_line_order   FOREIGN KEY (order_id)   REFERENCES "order"(id)   ON DELETE CASCADE,
    CONSTRAINT fk_line_product FOREIGN KEY (product_id) REFERENCES product(id)   ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_line_order   ON order_line (order_id);
CREATE INDEX IF NOT EXISTS idx_line_product ON order_line (product_id);
