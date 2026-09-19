-- ===================================================================
-- ENTERPRISE ORDER & INVENTORY MANAGEMENT PLATFORM
-- MySQL Database Setup & Schema Reference
-- Compatible with MySQL 8.0+
-- ===================================================================

-- 1. Schema Definition (Hibernate auto-generates with ddl-auto: update)
-- Roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- User Roles Junction
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Categories
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active TINYINT(1) DEFAULT 1 NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- Products
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(12,2) NOT NULL,
    cost_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Warehouses
CREATE TABLE warehouses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    active TINYINT(1) DEFAULT 1 NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- Inventories
CREATE TABLE inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    quantity BIGINT DEFAULT 0 NOT NULL,
    reserved_quantity BIGINT DEFAULT 0 NOT NULL,
    reorder_level BIGINT DEFAULT 10 NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_inventories_prod_wh UNIQUE (product_id, warehouse_id),
    CONSTRAINT fk_inventories_product_id FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_inventories_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Inventory Transactions Audit Ledger
CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    quantity BIGINT NOT NULL,
    reference_id VARCHAR(100),
    notes VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_inv_tx_inventory_id FOREIGN KEY (inventory_id) REFERENCES inventories(id)
);

-- Orders
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING' NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    tax DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    billing_address VARCHAR(500),
    notes VARCHAR(500),
    version BIGINT DEFAULT 0 NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_orders_customer_id FOREIGN KEY (customer_id) REFERENCES users(id)
);

-- Order Items
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity BIGINT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product_id FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Order Status History
CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100),
    reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_order_hist_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
