package com.example.crm.config;

import com.example.crm.service.ContactService;
import com.example.crm.service.CustomerService;
import com.holonplatform.auth.AccountProvider;
import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.Realm;
import com.holonplatform.auth.jdbc.DatastoreAccountProvider;
import com.holonplatform.auth.keys.AccountCredentialsAuthenticator;
import com.holonplatform.core.Context;
import com.holonplatform.core.ContextScope;
import com.holonplatform.core.datastore.Datastore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Holon wiring for the CRM demo.
 *
 * <p>Services are plain classes published into the Holon {@link Context} — no
 * {@code @Service}/{@code @Autowired}. Auth is Holon Auth ({@link Realm}), not Spring
 * Security. The only Spring annotations used here are {@code @Configuration}/{@code @Bean},
 * which merely return resources for Holon to manage.</p>
 */
@Configuration
public class CrmConfig {

    /**
     * Register the domain services into the Holon Context once the Datastore is available.
     * Uses {@link ApplicationReadyEvent} so all Holon starters have already published the
     * {@link Datastore} into the Context.
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> registerServices() {
        return event -> {
            Context.get().scope(ContextScope.APPLICATION)
                    .registerResource(CustomerService.CONTEXT_KEY, new CustomerService(), CustomerService.class);
            Context.get().scope(ContextScope.APPLICATION)
                    .registerResource(ContactService.CONTEXT_KEY, new ContactService(), ContactService.class);
        };
    }

    /**
     * Holon Auth Realm. Accounts, roles and permissions are loaded from the
     * {@code holon_account} / {@code holon_role} / {@code holon_permission} tables
     * created by {@code V003__auth_schema.sql}.
     */
    @Bean
    public AccountProvider accountProvider(Datastore datastore) {
        return DatastoreAccountProvider.create(datastore);
    }

    @Bean
    public Realm realm(AccountProvider accountProvider) {
        return Realm.builder()
                .withAuthenticator(AccountCredentialsAuthenticator.create(accountProvider))
                .withAuthorizer(Authorizer.create())
                .withDefaultAuthorization()
                .build();
    }
}
