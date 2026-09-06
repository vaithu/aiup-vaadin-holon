-- V001__create_tenant_table.sql
-- Platform-schema table backing com.holonplatform.multitenant.TenantDetails (tenant-core)

CREATE TABLE tenant
(
    tenant_id VARCHAR(64)  NOT NULL PRIMARY KEY,
    name      VARCHAR(255),
    plan      VARCHAR(32)  NOT NULL,
    status    VARCHAR(32)  NOT NULL,
    locale    VARCHAR(32),
    timezone  VARCHAR(64),
    theme     VARCHAR(64)
);

CREATE INDEX idx_tenant_status ON tenant (status);
CREATE INDEX idx_tenant_plan   ON tenant (plan);

-- @ElementCollection tables (Set<String> features / Map<String,String> attributes)
CREATE TABLE tenant_features
(
    tenant_id   VARCHAR(64)  NOT NULL REFERENCES tenant (tenant_id),
    feature_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (tenant_id, feature_key)
);

CREATE TABLE tenant_attributes
(
    tenant_id  VARCHAR(64)  NOT NULL REFERENCES tenant (tenant_id),
    attr_key   VARCHAR(255) NOT NULL,
    attr_value TEXT,
    PRIMARY KEY (tenant_id, attr_key)
);