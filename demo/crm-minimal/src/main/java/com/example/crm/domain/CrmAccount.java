package com.example.crm.domain;

import com.holonplatform.core.beans.DataPath;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Login credentials for a tenant's user, stored in the platform (shared) schema — not the
 * per-tenant schema. This class is part of the CON-S-010 platform-boundary exemption: SaaS
 * authentication storage uses plain Spring Data JPA / Hibernate rather than the Holon
 * Datastore, matching {@code holon-saas}'s own {@code AccountCredentialsStore} SPI contract.
 *
 * <p>{@code passwordHash} always arrives already BCrypt-encoded — {@link
 * com.holonplatform.multitenant.signup.SignupService} hashes the raw password internally
 * before ever calling {@link com.holonplatform.multitenant.signup.AccountCredentialsStore#save}.
 */
@Entity(name = "crmaccount")
@Table(name = "crm_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crm_account_tenant_email", columnNames = {"tenant_id", "email"})
})
public class CrmAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_account_seq")
    @SequenceGenerator(name = "crm_account_seq", sequenceName = "crm_account_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public CrmAccount() {
        // JPA
    }

    public CrmAccount(String tenantId, String email, String passwordHash) {
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}