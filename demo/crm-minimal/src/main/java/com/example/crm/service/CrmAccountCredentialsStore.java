package com.example.crm.service;

import com.example.crm.domain.CrmAccount;
import com.holonplatform.multitenant.signup.AccountCredentialsStore;
import org.springframework.stereotype.Component;

@Component
public class CrmAccountCredentialsStore implements AccountCredentialsStore {

    private final CrmAccountDatastoreHelper datastoreHelper;

    public CrmAccountCredentialsStore(CrmAccountDatastoreHelper datastoreHelper) {
        this.datastoreHelper = datastoreHelper;
    }

    @Override
    public boolean existsByEmail(String tenantId, String email) {
        return datastoreHelper.existsByTenantIdAndEmail(tenantId, email);
    }

    @Override
    public void save(String tenantId, String email, String encodedPassword) {
        // encodedPassword is already BCrypt-hashed by SignupService — never the raw password
        datastoreHelper.save(new CrmAccount(tenantId, email, encodedPassword));
    }
}