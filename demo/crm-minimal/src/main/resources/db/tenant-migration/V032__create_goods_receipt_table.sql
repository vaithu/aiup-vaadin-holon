-- V031__create_goods_receipt_table.sql
-- Entity: GOODS_RECEIPT (docs/entity_model.md)

CREATE SEQUENCE goods_receipt_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE goods_receipt
(
    id BIGINT DEFAULT nextval('goods_receipt_seq') PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_order(id),
    receipt_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    received_at TIMESTAMP NOT NULL,
    received_by VARCHAR(50) NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);

-- received_by: references holon-saas tenant-users TenantUser.userId (external, not enforced by FK)
