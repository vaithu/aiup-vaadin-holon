package com.example.crm.domain;

import com.holonplatform.core.property.StringProperty;

/**
 * Holon property-set definitions for {@link CrmAccount}, used by {@link
 * com.example.crm.service.CrmAccountDatastoreHelper} to build type-safe query filters.
 * Property names must match the JavaBean getter/setter names on {@link CrmAccount}.
 */
public interface CrmAccountProperties {

    StringProperty TENANT_ID = StringProperty.create("tenantId");
    StringProperty EMAIL = StringProperty.create("email");
}