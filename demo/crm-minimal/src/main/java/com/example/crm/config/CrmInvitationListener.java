package com.example.crm.config;

import com.example.crm.domain.CrmAccount;
import com.example.crm.service.CrmAccountDatastoreHelper;
import com.holonplatform.multitenant.invitations.InvitationAcceptedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CrmInvitationListener {

    private final PasswordEncoder passwordEncoder;
    private final CrmAccountDatastoreHelper accountDatastoreHelper;

    public CrmInvitationListener(PasswordEncoder passwordEncoder,
                                  CrmAccountDatastoreHelper accountDatastoreHelper) {
        this.passwordEncoder = passwordEncoder;
        this.accountDatastoreHelper = accountDatastoreHelper;
    }

    @EventListener
    public void onInvitationAccepted(InvitationAcceptedEvent event) {
        String tenantId = event.getInvitation().tenantId();
        String email = event.getTenantUser().getEmail();
        String encoded = passwordEncoder.encode(event.getRawPassword());
        accountDatastoreHelper.save(new CrmAccount(tenantId, email, encoded));
    }
}