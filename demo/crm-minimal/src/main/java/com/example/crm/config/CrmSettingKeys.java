package com.example.crm.config;

import com.holonplatform.multitenant.settings.TenantSetting;

/**
 * IyenSoft-specific tenant settings beyond the built-in {@code TenantSettingKeys}
 * (branding/rate-limits/email/retention). Complex domain-owned configuration —
 * numbering patterns (FR-T-003), tax codes (FR-T-004), fiscal year (FR-T-005) — gets its
 * own relational table per docs/entity_model.md and is NOT stored here; these are simple
 * scalar preferences that fit the key-value model.
 */
public final class CrmSettingKeys {
    private CrmSettingKeys() {}

    public static final TenantSetting DEFAULT_CURRENCY =
            TenantSetting.of("erp.default_currency", "USD", "Default transaction currency");

    public static final TenantSetting DATE_FORMAT =
            TenantSetting.of("erp.date_format", "yyyy-MM-dd", "Preferred date display format");

    public static final TenantSetting NUMBER_FORMAT =
            TenantSetting.of("erp.number_format", "#,##0.00", "Preferred number display format");
}