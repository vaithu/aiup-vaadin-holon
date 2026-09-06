-- V015__create_stock_adjustment_table.sql
-- Entity: STOCK_ADJUSTMENT (docs/entity_model.md)

CREATE SEQUENCE stock_adjustment_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE stock_adjustment
(
    id BIGINT DEFAULT nextval('stock_adjustment_seq') PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    bin_id BIGINT NOT NULL REFERENCES bin(id),
    quantity_delta DECIMAL(12,2) NOT NULL,
    reason_code BIGINT NOT NULL REFERENCES reference_code(id),
    adjusted_by VARCHAR(50) NOT NULL,
    adjusted_at TIMESTAMP NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);

-- adjusted_by: references holon-saas tenant-users TenantUser.userId (external, not enforced by FK)
