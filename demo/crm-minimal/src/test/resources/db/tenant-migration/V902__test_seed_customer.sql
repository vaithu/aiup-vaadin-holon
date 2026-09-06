-- V902__test_seed_customer.sql
-- Test-only seed data for UC-S-001 "Create Customer" Vaadin Browserless tests.
-- Depends on the tax_code rows seeded by V901__test_seed_tax_code.sql (default_tax_code_id
-- is NOT NULL / FK, per V010__create_customer_table.sql).

INSERT INTO customer
    (id, name, customer_number, billing_address, tax_id, default_currency, default_tax_code_id,
     payment_terms_days, created_by, created_date)
VALUES
    (9001, 'Acme Robotics Ltd', 'CUST-009001', '1 Innovation Way, Austin, TX 78701', 'US-TAX-0001',
     'USD', 9001, 30, 'test-seed', CURRENT_TIMESTAMP),
    (9002, 'Blue Harbor Foods', 'CUST-009002', '22 Harbor Road, Portland, OR 97201', 'US-TAX-0002',
     'USD', 9002, 45, 'test-seed', CURRENT_TIMESTAMP),
    (9003, 'Nordic Timber AB', 'CUST-009003', 'Skeppsbron 4, 111 30 Stockholm', 'SE-TAX-0003',
     'EUR', 9001, 60, 'test-seed', CURRENT_TIMESTAMP);
