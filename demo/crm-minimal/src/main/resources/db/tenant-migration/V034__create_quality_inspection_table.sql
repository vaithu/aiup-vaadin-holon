-- V033__create_quality_inspection_table.sql
-- Entity: QUALITY_INSPECTION (docs/entity_model.md)

CREATE SEQUENCE quality_inspection_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE quality_inspection
(
    id BIGINT DEFAULT nextval('quality_inspection_seq') PRIMARY KEY,
    goods_receipt_id BIGINT NOT NULL REFERENCES goods_receipt(id),
    inspector_id VARCHAR(50) NOT NULL,
    sample_size INTEGER NOT NULL CHECK (sample_size BETWEEN 1 AND 9999),
    result_code BIGINT NOT NULL REFERENCES reference_code(id),
    inspected_at TIMESTAMP NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);

-- inspector_id: references holon-saas tenant-users TenantUser.userId (external, not enforced by FK)
