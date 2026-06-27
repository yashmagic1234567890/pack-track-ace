-- =====================================================================
-- FreshTrack — MySQL schema (reference DDL)
-- The application uses Hibernate `ddl-auto: update` to create these tables
-- automatically. This file documents the schema and can be applied manually.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS freshtrack
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE freshtrack;

-- ------------------------- Warehouses --------------------------------
CREATE TABLE IF NOT EXISTS warehouses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(50)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    location       VARCHAR(250),
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT uk_warehouse_code UNIQUE (warehouse_code)
);

-- ---------------------------- Users ----------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(150),
    role       VARCHAR(30)  NOT NULL,
    enabled    BIT(1)       NOT NULL DEFAULT b'1',
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

-- -------------------- User <-> Warehouse mapping ---------------------
CREATE TABLE IF NOT EXISTS user_warehouses (
    user_id      BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, warehouse_id),
    CONSTRAINT fk_uw_user      FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_uw_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

-- --------------------------- Invoices --------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_business_id VARCHAR(80)  NOT NULL,
    vendor_name         VARCHAR(150) NOT NULL,
    warehouse_id        BIGINT       NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    uploaded_by         VARCHAR(100),
    created_at          DATETIME(6)  NOT NULL,
    CONSTRAINT uk_invoice_business_id UNIQUE (invoice_business_id),
    CONSTRAINT fk_invoice_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- ------------------------- Invoice lines -----------------------------
CREATE TABLE IF NOT EXISTS invoice_lines (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id        BIGINT       NOT NULL,
    item_sku          VARCHAR(100) NOT NULL,
    item_name         VARCHAR(200) NOT NULL,
    expected_quantity INT          NOT NULL,
    received_quantity INT          NOT NULL DEFAULT 0,
    version           BIGINT,
    updated_at        DATETIME(6),
    CONSTRAINT fk_line_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT uk_invoice_sku UNIQUE (invoice_id, item_sku)
);
CREATE INDEX idx_line_invoice ON invoice_lines (invoice_id);
CREATE INDEX idx_line_sku     ON invoice_lines (item_sku);

-- --------------------------- Audit logs ------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type         VARCHAR(40)  NOT NULL,
    invoice_business_id VARCHAR(80),
    item_sku            VARCHAR(100),
    warehouse_code      VARCHAR(50),
    username            VARCHAR(100) NOT NULL,
    quantity_delta      INT,
    resulting_quantity  INT,
    details             VARCHAR(500),
    created_at          DATETIME(6)  NOT NULL
);
CREATE INDEX idx_audit_invoice ON audit_logs (invoice_business_id);
CREATE INDEX idx_audit_user    ON audit_logs (username);
CREATE INDEX idx_audit_ts      ON audit_logs (created_at);
