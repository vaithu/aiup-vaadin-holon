package com.example.crm.config;

import com.example.crm.domain.CrmAccount;
import com.example.crm.domain.CrmAccountProperties;
import com.holonplatform.core.datastore.beans.BeanDatastoreHelper;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.multitenant.TenantContext;
import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.multitenant.security.TenantUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Loads Spring Security {@link UserDetails} for the current tenant, resolved from
 * {@link TenantContext} (already populated by {@code TenantContextFilter} before
 * Spring Security's authentication filters run — see Step 2's PATH resolution).
 */
@Component
public class CrmUserDetailsService implements UserDetailsService {

    private final BeanDatastoreHelper<CrmAccount> accountHelper;

    public CrmUserDetailsService(com.holonplatform.core.datastore.Datastore datastore) {
        this.accountHelper = BeanDatastoreHelper.of(datastore, CrmAccount.class);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        TenantDetails tenant = TenantContext.require(); // throws if no tenant resolved

        CrmAccount account = accountHelper.findTop(1,
                        QueryFilter.eq(CrmAccountProperties.TENANT_ID, tenant.tenantId())
                                .and(QueryFilter.eq(CrmAccountProperties.EMAIL, email)))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return TenantUserDetails.builder(account.getEmail(), tenant)
                .password(account.getPasswordHash())
                .role("USER")
                .build();
    }
}