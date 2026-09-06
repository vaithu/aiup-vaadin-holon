package com.example.crm.config;

import com.holonplatform.multitenant.TenantPlan;
import com.holonplatform.multitenant.limits.TenantRateLimiter;
import com.holonplatform.multitenant.limits.internal.TokenBucketRateLimiter;
import com.holonplatform.multitenant.quotas.QuotaDefinition;
import com.holonplatform.multitenant.quotas.QuotaKeys;
import com.holonplatform.multitenant.quotas.TenantQuotaService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Registers IyenSoft-specific rate-limit keys and quota definitions on top of the
 * built-in defaults auto-configured by {@code tenant-limits}/{@code tenant-quotas}.
 *
 * <p>{@code QuotaKeys.MAX_USERS} is immediately functional — {@code UserCountQuotaProvider}
 * (auto-registered) already counts real {@code TenantUser} rows. The other quota keys
 * registered here (MAX_CONTACTS, MAX_ACTIVE_DEALS, MAX_STORAGE_MB) will report a limit but
 * cannot enforce actual usage until a matching {@code QuotaUsageProvider} bean exists —
 * that requires the domain layer's repositories, which don't exist yet. See the pattern
 * comment in the guide for exactly what to add once they do.
 *
 * <p>{@link TokenBucketRateLimiter} (the concrete class the tenant-limits auto-configuration
 * registers) is injected directly rather than the {@link TenantRateLimiter} interface —
 * {@code registerDefinition(...)} is a concrete-class-only method, not part of the interface
 * contract.
 */
@Component
public class CrmLimitsAndQuotasConfig {

    private final TokenBucketRateLimiter rateLimiter;
    private final TenantQuotaService quotaService;

    public CrmLimitsAndQuotasConfig(TokenBucketRateLimiter rateLimiter, TenantQuotaService quotaService) {
        this.rateLimiter = rateLimiter;
        this.quotaService = quotaService;
    }

    @PostConstruct
    void configure() {
        // ── Rate limits ──────────────────────────────────────────────────
        // Built-in API_REQUESTS/EMAIL_SENDS/FILE_UPLOADS/REPORT_EXPORTS/WEBHOOK_DELIVERIES
        // already have sensible defaults registered automatically. Add IyenSoft-specific keys:
        rateLimiter.registerDefinition("erp.bulk_import",
                TenantRateLimiter.LimitDefinition.perHour(10));
        rateLimiter.registerDefinition("erp.payment_batch_submit",
                TenantRateLimiter.LimitDefinition.of(5, Duration.ofMinutes(10)));

        // ── Quotas ───────────────────────────────────────────────────────
        quotaService.registerQuota(QuotaDefinition.builder(QuotaKeys.MAX_USERS)
                .limit(TenantPlan.FREE, 3L)
                .limit(TenantPlan.STARTER, 10L)
                .limit(TenantPlan.PROFESSIONAL, 50L)
                .limit(TenantPlan.ENTERPRISE, -1L)
                .build());

        quotaService.registerQuota(QuotaDefinition.builder(QuotaKeys.MAX_CONTACTS)
                .limit(TenantPlan.FREE, 500L)
                .limit(TenantPlan.STARTER, 5_000L)
                .limit(TenantPlan.PROFESSIONAL, 50_000L)
                .limit(TenantPlan.ENTERPRISE, -1L)
                .build());

        quotaService.registerQuota(QuotaDefinition.builder(QuotaKeys.MAX_STORAGE_MB)
                .limit(TenantPlan.FREE, 1_000L)
                .limit(TenantPlan.STARTER, 10_000L)
                .limit(TenantPlan.PROFESSIONAL, 100_000L)
                .limit(TenantPlan.ENTERPRISE, -1L)
                .build());
    }
}