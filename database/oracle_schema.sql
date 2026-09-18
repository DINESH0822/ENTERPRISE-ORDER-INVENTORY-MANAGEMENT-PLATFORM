-- ===================================================================
-- ENTERPRISE ORDER & INVENTORY MANAGEMENT PLATFORM
-- Oracle Database Setup & Schema Reference
-- Compatible with Oracle Database 19c / 21c / 23c / XE
-- ===================================================================

-- 1. Create Tablespace & User (Run as SYSDBA if creating dedicated schema)
/*
CREATE USER ENTERPRISE_DB IDENTIFIED BY "Enterprise#2026"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON USERS;

GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE TABLE TO ENTERPRISE_DB;
*/

-- 2. Schema Definition (Hibernate auto-generates with ddl-auto: update)
-- Roles
CREATE TABLE roles (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(50) NOT NULL UNIQUE,
    description VARCHAR2(255)
);

-- Users
CREATE TABLE users (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR2(100) NOT NULL,
    last_name VARCHAR2(100) NOT NULL,
    email VARCHAR2(150) NOT NULL UNIQUE,
    password VARCHAR2(255) NOT NULL,
    phone VARCHAR2(30),
    status VARCHAR2(30) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- User Roles Junction
CREATE TABLE user_roles (
    user_id NUMBER(19) NOT NULL,
    role_id NUMBER(19) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Categories
CREATE TABLE categories (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(100) NOT NULL UNIQUE,
    description VARCHAR2(500),
    active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Products
CREATE TABLE products (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku VARCHAR2(50) NOT NULL UNIQUE,
    name VARCHAR2(200) NOT NULL,
    description VARCHAR2(1000),
    price NUMBER(12,2) NOT NULL,
    cost_price NUMBER(12,2) NOT NULL,
    status VARCHAR2(30) DEFAULT 'ACTIVE' NOT NULL,
    category_id NUMBER(19) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Warehouses
CREATE TABLE warehouses (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR2(50) NOT NULL UNIQUE,
    name VARCHAR2(150) NOT NULL,
    location VARCHAR2(255) NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Inventories
CREATE TABLE inventories (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id NUMBER(19) NOT NULL,
    warehouse_id NUMBER(19) NOT NULL,
    quantity NUMBER(19) DEFAULT 0 NOT NULL,
    reserved_quantity NUMBER(19) DEFAULT 0 NOT NULL,
    reorder_level NUMBER(19) DEFAULT 10 NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inventories_prod_wh UNIQUE (product_id, warehouse_id),
    CONSTRAINT fk_inventories_product_id FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_inventories_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Inventory Transactions Audit Ledger
CREATE TABLE inventory_transactions (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id NUMBER(19) NOT NULL,
    transaction_type VARCHAR2(30) NOT NULL,
    quantity NUMBER(19) NOT NULL,
    reference_id VARCHAR2(100),
    notes VARCHAR2(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_inv_tx_inventory_id FOREIGN KEY (inventory_id) REFERENCES inventories(id)
);

-- Orders
CREATE TABLE orders (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number VARCHAR2(50) NOT NULL UNIQUE,
    customer_id NUMBER(19) NOT NULL,
    status VARCHAR2(30) DEFAULT 'PENDING' NOT NULL,
    subtotal NUMBER(12,2) NOT NULL,
    tax NUMBER(12,2) NOT NULL,
    shipping_fee NUMBER(12,2) NOT NULL,
    total_amount NUMBER(12,2) NOT NULL,
    shipping_address VARCHAR2(500) NOT NULL,
    billing_address VARCHAR2(500),
    notes VARCHAR2(500),
    version NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_orders_customer_id FOREIGN KEY (customer_id) REFERENCES users(id)
);

-- Order Items
CREATE TABLE order_items (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id NUMBER(19) NOT NULL,
    product_id NUMBER(19) NOT NULL,
    warehouse_id NUMBER(19) NOT NULL,
    unit_price NUMBER(12,2) NOT NULL,
    quantity NUMBER(19) NOT NULL,
    subtotal NUMBER(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product_id FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Order Status History
CREATE TABLE order_status_history (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id NUMBER(19) NOT NULL,
    previous_status VARCHAR2(30),
    new_status VARCHAR2(30) NOT NULL,
    changed_by VARCHAR2(100),
    reason VARCHAR2(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_hist_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
