package com.example.crm.service;

import com.holonplatform.multitenant.TenantPlan;
import com.holonplatform.multitenant.billing.Invoice;
import com.holonplatform.multitenant.billing.InvoiceStatus;
import com.holonplatform.multitenant.billing.TenantBillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class CrmBillingHandler {

    @Autowired
    private TenantBillingService billingService;

    /** Called from your Stripe (or other provider) webhook handler once payment succeeds. */
    public void handlePaymentSucceeded(String tenantId, String invoiceId, BigDecimal amount, String currency) {
        billingService.saveInvoice(Invoice.builder(invoiceId, tenantId)
                .amountDue(amount)
                .currency(currency)
                .status(InvoiceStatus.PAID)
                .issuedAt(Instant.now())
                .paidAt(Instant.now())
                .build());
    }

    /** Upgrades a tenant's subscription plan — also updates TenantDetails so @RequiresPlan
     *  gates (Step 5) respect the new plan immediately. */
    public void upgradePlan(String tenantId, TenantPlan newPlan) {
        billingService.changePlan(tenantId, newPlan);
    }

    public void cancelSubscription(String tenantId) {
        billingService.cancel(tenantId);
    }
}