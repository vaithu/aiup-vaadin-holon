package com.example.crm.config;

import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.multitenant.admin.service.TenantAdminService;
import com.holonplatform.multitenant.billing.TenantBillingService;
import com.holonplatform.multitenant.onboarding.event.TenantOnboardingCompletedEvent;
import com.holonplatform.multitenant.onboarding.event.TenantOnboardingFailedEvent;
import com.holonplatform.multitenant.settings.TenantSettingKeys;
import com.holonplatform.multitenant.settings.TenantSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CrmOnboardingListener {

    private static final Logger log = LoggerFactory.getLogger(CrmOnboardingListener.class);

    private final TenantAdminService tenantAdminService;
    private final TenantBillingService tenantBillingService;
    // Injected as ObjectProvider so this class compiles and runs correctly whether or not
    // Step 9 (tenant-settings wiring) has been done yet — getIfAvailable() returns null
    // until then, and the branding block below is simply skipped.
    private final ObjectProvider<TenantSettingsService> tenantSettingsServiceProvider;

    public CrmOnboardingListener(TenantAdminService tenantAdminService,TenantBillingService tenantBillingService,
                                  ObjectProvider<TenantSettingsService> tenantSettingsServiceProvider) {
        this.tenantAdminService = tenantAdminService;
        this.tenantBillingService = tenantBillingService;
        this.tenantSettingsServiceProvider = tenantSettingsServiceProvider;
    }

    @EventListener
    public void onOnboarded(TenantOnboardingCompletedEvent event) {
        TenantDetails tenant = event.getTenant();
        Map<String, String> attrs = tenant.attributes();

        // Regional data (Step 3 of the wizard): promote from generic attributes to
        // first-class TenantDetails fields. TenantDetails lives in the PLATFORM schema
        // (via TenantDetailsRepository, a plain JpaRepository) — this call does NOT need
        // TenantContext bound.
        tenantAdminService.save(tenant.toBuilder()
                .locale(attrs.get("locale"))
                .timezone(attrs.get("timezone"))
                .build());

        // Everything below writes through the tenant-scoped Datastore (TenantAwareDataSource),
        // which routes by TenantContext.peekTenantId() — NOT by any tenantId argument passed
        // explicitly. This listener fires SYNCHRONOUSLY from inside
        // DefaultTenantOnboardingService.onboard() (Spring's ApplicationEventPublisher is
        // synchronous by default), which happens BEFORE DefaultSignupService ever binds
        // TenantContext. TenantContextTemplate.run(...) handles the set()/finally-clear()
        // boilerplate correctly (restores rather than blindly clears, if ever nested).
        com.holonplatform.multitenant.TenantContextTemplate.run(tenant, () -> {
            // Branding data (Step 2 of the wizard): belongs in tenant-settings, not raw
            // tenant attributes. No-op until Step 9 wires a TenantSettingsService bean.
            TenantSettingsService settingsService = tenantSettingsServiceProvider.getIfAvailable();
            if (settingsService != null) {
                String tenantId = event.getTenantId();
                if (attrs.get("logoUrl") != null) {
                    settingsService.set(tenantId, TenantSettingKeys.LOGO_URL, attrs.get("logoUrl"));
                }
                if (attrs.get("primaryColor") != null) {
                    settingsService.set(tenantId, TenantSettingKeys.PRIMARY_COLOR, attrs.get("primaryColor"));
                }
            } else {
                log.debug("TenantSettingsService not yet configured — skipping branding for tenant '{}'",
                        event.getTenantId());
            }

            tenantBillingService.save(com.holonplatform.multitenant.billing.Subscription.builder(event.getTenantId())
                    .plan(tenant.plan())
                    .status(com.holonplatform.multitenant.billing.SubscriptionStatus.ACTIVE)
                    .periodStart(java.time.Instant.now())
                    .build());
        });
    }

    @EventListener
    public void onFailed(TenantOnboardingFailedEvent event) {
        log.error("Onboarding failed for tenant '{}'", event.getTenantId(), event.getCause());
    }
}