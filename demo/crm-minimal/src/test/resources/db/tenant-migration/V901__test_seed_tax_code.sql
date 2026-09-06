-- V901__test_seed_tax_code.sql
-- Test-only seed data for UC-S-001 "Create Customer" Vaadin Browserless tests.
-- Explicit ids (9001+) are used so they never collide with customer_seq/tax_code_seq
-- generated values (1, 51, 101, ...) produced by CustomerService.create()/TaxCodeService
-- during the same test run.

INSERT INTO tax_code (id, code, rate, is_default, is_active, created_by, created_date)
VALUES
    (9001, 'VAT-STD', 20.00, TRUE, TRUE, 'test-seed', CURRENT_TIMESTAMP),
    (9002, 'VAT-RED', 5.00, FALSE, TRUE, 'test-seed', CURRENT_TIMESTAMP),
    (9003, 'VAT-OLD', 17.50, FALSE, FALSE, 'test-seed', CURRENT_TIMESTAMP);
