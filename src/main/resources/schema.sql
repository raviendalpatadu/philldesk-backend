-- PhillDesk Database Schema
-- This file contains the database schema for the PhillDesk Online Pharmacy Management System

-- Create Database (run this separately if needed)
-- CREATE DATABASE philldesk_db;

-- ==============================================
-- ROLES TABLE
-- ==============================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(500)
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES 
('ADMIN', 'System administrator with full access'),
('PHARMACIST', 'Pharmacist who can approve prescriptions and manage inventory'),
('CUSTOMER', 'Customer who can upload prescriptions and view order history');

-- ==============================================
-- USERS TABLE
-- ==============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(15),
    address VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================
-- MEDICINES TABLE
-- ==============================================
CREATE TABLE medicines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    manufacturer VARCHAR(100),
    category VARCHAR(50),
    dosage_form VARCHAR(50),
    strength VARCHAR(50),
    quantity INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2),
    expiry_date DATE,
    batch_number VARCHAR(50),
    reorder_level INTEGER NOT NULL DEFAULT 10,
    description VARCHAR(500),
    is_prescription_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================
-- PRESCRIPTIONS TABLE
-- ==============================================
CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    prescription_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    pharmacist_id BIGINT REFERENCES users(id),
    doctor_name VARCHAR(100),
    doctor_license VARCHAR(50),
    prescription_date TIMESTAMP,
    file_url VARCHAR(500),
    file_name VARCHAR(200),
    file_type VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(1000),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    CONSTRAINT chk_prescription_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DISPENSED', 'COMPLETED', 'READY_FOR_PICKUP'))
);

-- ==============================================
-- PRESCRIPTION_ITEMS TABLE
-- ==============================================
CREATE TABLE prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id),
    quantity INTEGER NOT NULL,
    dosage VARCHAR(500),
    frequency VARCHAR(100),
    instructions VARCHAR(200),
    unit_price DECIMAL(10,2),
    total_price DECIMAL(10,2),
    is_dispensed BOOLEAN NOT NULL DEFAULT FALSE
);

-- ==============================================
-- BILLS TABLE
-- ==============================================
CREATE TABLE bills (
    id BIGSERIAL PRIMARY KEY,
    bill_number VARCHAR(50) UNIQUE NOT NULL,
    prescription_id BIGINT REFERENCES prescriptions(id),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    pharmacist_id BIGINT NOT NULL REFERENCES users(id),
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(10,2) DEFAULT 0.00,
    tax DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'PARTIALLY_PAID', 'CANCELLED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'CARD','ONLINE', 'BANK_TRANSFER', 'OTHER'))
);

-- ==============================================
-- BILL_ITEMS TABLE
-- ==============================================
CREATE TABLE bill_items (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    batch_number VARCHAR(50),
    notes VARCHAR(200)
);

-- ==============================================
-- NOTIFICATIONS TABLE
-- ==============================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT chk_notification_type CHECK (notification_type IN ('LOW_STOCK', 'EXPIRY_ALERT', 'PRESCRIPTION_UPLOADED', 'PRESCRIPTION_APPROVED', 'PRESCRIPTION_REJECTED', 'BILL_GENERATED', 'SYSTEM_ALERT', 'USER_REGISTRATION')),
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- ==============================================
-- INDEXES FOR PERFORMANCE
-- ==============================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role_id ON users(role_id);

CREATE INDEX idx_medicines_name ON medicines(name);
CREATE INDEX idx_medicines_category ON medicines(category);
CREATE INDEX idx_medicines_expiry_date ON medicines(expiry_date);
CREATE INDEX idx_medicines_quantity ON medicines(quantity);

CREATE INDEX idx_prescriptions_customer_id ON prescriptions(customer_id);
CREATE INDEX idx_prescriptions_pharmacist_id ON prescriptions(pharmacist_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status);
CREATE INDEX idx_prescriptions_created_at ON prescriptions(created_at);

CREATE INDEX idx_bills_customer_id ON bills(customer_id);
CREATE INDEX idx_bills_pharmacist_id ON bills(pharmacist_id);
CREATE INDEX idx_bills_payment_status ON bills(payment_status);
CREATE INDEX idx_bills_created_at ON bills(created_at);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_type ON notifications(notification_type);

-- ==============================================
-- SAMPLE DATA FOR TESTING
-- ==============================================

-- Sample Admin User (password should be hashed in real application)
INSERT INTO users (username, email, password, first_name, last_name, phone, address, role_id) 
VALUES ('admin', 'admin@philldesk.com', '$2a$10$example', 'System', 'Administrator', '+94771234567', 'Colombo, Sri Lanka', 1);

-- Sample Pharmacist
INSERT INTO users (username, email, password, first_name, last_name, phone, address, role_id) 
VALUES ('pharmacist1', 'pharmacist@philldesk.com', '$2a$10$example', 'Dr. John', 'Perera', '+94772345678', 'Kandy, Sri Lanka', 2);

-- Sample Customer
INSERT INTO users (username, email, password, first_name, last_name, phone, address, role_id) 
VALUES ('customer1', 'customer@example.com', '$2a$10$example', 'Nimal', 'Silva', '+94773456789', 'Galle, Sri Lanka', 3);

-- Sample Medicines
INSERT INTO medicines (name, generic_name, manufacturer, category, dosage_form, strength, quantity, unit_price, cost_price, expiry_date, reorder_level, is_prescription_required) VALUES 
('Panadol', 'Paracetamol', 'GSK', 'Analgesic', 'Tablet', '500mg', 100, 25.00, 20.00, '2025-12-31', 20, FALSE),
('Amoxil', 'Amoxicillin', 'GSK', 'Antibiotic', 'Capsule', '250mg', 50, 15.50, 12.00, '2025-06-30', 15, TRUE),
('Voltaren', 'Diclofenac', 'Novartis', 'Anti-inflammatory', 'Tablet', '50mg', 75, 35.00, 28.00, '2025-09-15', 25, TRUE);
