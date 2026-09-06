package com.example.crm.service;

import com.holonplatform.multitenant.invitations.InvitationSender;
import com.holonplatform.multitenant.invitations.UserInvitation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Minimal stub {@link InvitationSender} — logs the invitation link instead of emailing it.
 * Replace with a real Spring Mail / SES / SendGrid implementation before production use.
 */
@Component
public class CrmInvitationSender implements InvitationSender {

    private static final Logger log = LoggerFactory.getLogger(CrmInvitationSender.class);

    @Override
    public void send(UserInvitation invitation, String invitationLink) {
        log.info("Invitation for tenant '{}' -> {} : {}",
                invitation.tenantId(), invitation.email(), invitationLink);
    }
}