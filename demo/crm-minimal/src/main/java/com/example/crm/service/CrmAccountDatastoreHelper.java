package com.example.crm.service;

import com.example.crm.domain.CrmAccount;
import com.example.crm.domain.CrmAccountProperties;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.beans.BeanDatastoreHelper;
import com.holonplatform.core.query.QueryFilter;

public class CrmAccountDatastoreHelper {

    private final BeanDatastoreHelper<CrmAccount> helper;

    public CrmAccountDatastoreHelper(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(datastore, CrmAccount.class);
    }

    public boolean existsByTenantIdAndEmail(String tenantId, String email) {
        return helper.exists(
                QueryFilter.eq(CrmAccountProperties.TENANT_ID, tenantId)
                        .and(QueryFilter.eq(CrmAccountProperties.EMAIL, email)));
    }

    /**
     * Every tenant that has an account registered under this email — a cross-tenant,
     * platform-schema query (not restricted by TenantContext). Supports FR-T-007
     * (one person may have accounts in more than one tenant).
     */
    public java.util.List<String> findTenantIdsByEmail(String email) {
        return helper.findAll(QueryFilter.eq(CrmAccountProperties.EMAIL, email))
                .map(CrmAccount::getTenantId)
                .distinct()
                .toList();
    }

    /** The account for a specific tenant/email pair, if any. */
    public java.util.Optional<CrmAccount> findByTenantIdAndEmail(String tenantId, String email) {
        return helper.findTop(1,
                        QueryFilter.eq(CrmAccountProperties.TENANT_ID, tenantId)
                                .and(QueryFilter.eq(CrmAccountProperties.EMAIL, email)))
                .findFirst();
    }

    public CrmAccount save(CrmAccount account) {
        return helper.insert(account).getResult().orElse(account);
    }
}