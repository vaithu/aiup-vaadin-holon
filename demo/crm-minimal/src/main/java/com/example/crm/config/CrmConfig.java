package com.example.crm.config;

import com.example.crm.service.CrmAccountDatastoreHelper;
import com.holonplatform.core.datastore.Datastore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CrmConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CrmAccountDatastoreHelper crmAccountDatastoreHelper(Datastore datastore) {
        return new CrmAccountDatastoreHelper(datastore);
    }
}