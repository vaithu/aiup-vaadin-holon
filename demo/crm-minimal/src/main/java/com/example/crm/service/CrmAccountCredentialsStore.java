package com.example.crm.service;

import com.example.crm.domain.CrmAccount;
import com.holonplatform.multitenant.signup.AccountCredentialsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrmAccountCredentialsStore implements AccountCredentialsStore {

    @Autowired
    private CrmAccountDatastoreHelper datastoreHelper;

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