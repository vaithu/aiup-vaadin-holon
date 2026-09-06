package com.example.crm.service;

import com.holonplatform.multitenant.invitations.InvitationRequest;
import com.holonplatform.multitenant.invitations.TenantInvitationService;
import com.holonplatform.multitenant.invitations.UserInvitation;
import com.holonplatform.multitenant.users.TenantRole;
import com.holonplatform.multitenant.users.TenantUser;
import com.holonplatform.multitenant.users.TenantUserService;
import com.holonplatform.multitenant.users.TenantUserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class CrmUserService {

    @Autowired
    private TenantInvitationService invitationService;

    @Autowired
    private TenantUserService userService;

    public UserInvitation inviteSalesperson(String tenantId, String invitedByUserId, String email) {
        return invitationService.invite(InvitationRequest.builder(tenantId, email)
                .invitedByUserId(invitedByUserId)
                .platformRole(TenantRole.MEMBER)
                .validity(Duration.ofHours(72))
                .build());
    }

    public void promoteToAdmin(String tenantId, String userId) {
        userService.changeRole(tenantId, userId, TenantRole.ADMIN);
    }

    public List<TenantUser> listActiveUsers(String tenantId) {
        return userService.findByStatus(tenantId, TenantUserStatus.ACTIVE, 0, 50);
    }
}