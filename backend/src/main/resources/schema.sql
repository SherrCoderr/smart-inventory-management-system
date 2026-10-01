-- ==============================================================================
-- Smart Inventory Management System - Database Reference Schema (PostgreSQL)
-- NOTE: Hibernate is configured with ddl-auto for entity synchronization.
-- This file serves as an explicit architectural reference and documentation.
-- ==============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email ON users(email);

-- 2. Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_category_name ON categories(name);

-- 3. Suppliers Table
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_supplier_email ON suppliers(email);
CREATE INDEX IF NOT EXISTS idx_supplier_name ON suppliers(name);

-- 4. Products Table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    current_stock INT NOT NULL DEFAULT 0,
    min_stock_threshold INT NOT NULL DEFAULT 5,
    reorder_quantity INT NOT NULL DEFAULT 20,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chk_product_current_stock CHECK (current_stock >= 0),
    CONSTRAINT chk_product_min_threshold CHECK (min_stock_threshold >= 0),
    CONSTRAINT chk_product_reorder_qty CHECK (reorder_quantity >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_product_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_product_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_product_supplier ON products(supplier_id);
CREATE INDEX IF NOT EXISTS idx_product_current_stock ON products(current_stock);
CREATE INDEX IF NOT EXISTS idx_product_active ON products(active);

-- 5. Inventory Transactions (Immutable Audit Log)
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    reference_reason VARCHAR(255),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inv_txn_product ON inventory_transactions(product_id);
CREATE INDEX IF NOT EXISTS idx_inv_txn_user ON inventory_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_inv_txn_created_at ON inventory_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_inv_txn_type ON inventory_transactions(transaction_type);
