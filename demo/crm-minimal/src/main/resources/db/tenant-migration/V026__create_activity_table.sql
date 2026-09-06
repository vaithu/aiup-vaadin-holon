-- V025__create_activity_table.sql
-- Entity: ACTIVITY (docs/entity_model.md)

CREATE SEQUENCE activity_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE activity
(
    id BIGINT DEFAULT nextval('activity_seq') PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    app_user_id VARCHAR(50) NOT NULL,
    type_code BIGINT NOT NULL REFERENCES reference_code(id),
    occurred_at TIMESTAMP NOT NULL,
    notes VARCHAR(2000),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);

-- app_user_id: references holon-saas tenant-users TenantUser.userId (external, not enforced by FK)
