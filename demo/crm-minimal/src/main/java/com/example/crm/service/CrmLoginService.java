package com.example.crm.service;

import com.example.crm.domain.CrmAccount;
import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.multitenant.TenantDetailsLoader;
import com.holonplatform.multitenant.security.TenantUserDetails;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CrmLoginService {

    private final CrmAccountDatastoreHelper accountHelper;
    private final TenantDetailsLoader tenantDetailsLoader;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public CrmLoginService(CrmAccountDatastoreHelper accountHelper,
            TenantDetailsLoader tenantDetailsLoader,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository) {
        this.accountHelper = accountHelper;
        this.tenantDetailsLoader = tenantDetailsLoader;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    /** Every tenant that has an account registered under this email. */
    public List<String> findTenantsForEmail(String email) {
        return accountHelper.findTenantIdsByEmail(email);
    }

    /**
     * Verifies the password for the given tenant/email and, on success, authenticates the
     * user into Spring Security's session-backed SecurityContext.
     *
     * @return the signed-in tenant, or empty if the email/password/tenant combination is invalid
     */
    public Optional<TenantDetails> authenticate(String tenantId, String email, String rawPassword) {
        Optional<CrmAccount> accountOpt = accountHelper.findByTenantIdAndEmail(tenantId, email);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }
        CrmAccount account = accountOpt.get();
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            return Optional.empty();
        }

        TenantDetails tenant = tenantDetailsLoader.load(tenantId);
        if (!tenant.isActive()) {
            return Optional.empty();
        }

        signIn(tenant, account);
        return Optional.of(tenant);
    }

    private void signIn(TenantDetails tenant, CrmAccount account) {
        UserDetails principal = TenantUserDetails.builder(account.getEmail(), tenant)
                .password(account.getPasswordHash())
                .role("USER")
                .build();

        // We've already verified the password ourselves above, so build a pre-authenticated
        // token directly rather than going through AuthenticationManager/UserDetailsService
        // (which would need TenantContext already set — impossible on the generic /login page).
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        VaadinServletRequest request = (VaadinServletRequest) VaadinService.getCurrentRequest();
        VaadinServletResponse response = (VaadinServletResponse) VaadinService.getCurrentResponse();
        securityContextRepository.saveContext(context, request, response);
    }
}