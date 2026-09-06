// src/main/java/com/example/crm/config/IyenSoftFeatures.java
package com.example.crm.config;

public final class IyenSoftFeatures {
    private IyenSoftFeatures() {}

    public static final String QUALITY_MODULE   = "erp.quality_module";   // inspections, RMA
    public static final String MULTI_WAREHOUSE  = "erp.multi_warehouse";  // transfers, bins
    public static final String MULTI_CURRENCY   = "erp.multi_currency";   // FX revaluation (FR-F-009)
    public static final String COMPOSITE_ITEMS  = "erp.composite_items";  // kit/build BOM
    public static final String PAYMENT_BATCHES  = "erp.payment_batches";  // SEPA batch payments
}