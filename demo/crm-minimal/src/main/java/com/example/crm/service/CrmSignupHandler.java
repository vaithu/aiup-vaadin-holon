package com.example.crm.service;

import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.multitenant.TenantPlan;
import com.holonplatform.multitenant.signup.SignupRequest;
import com.holonplatform.multitenant.signup.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CrmSignupHandler {

    @Autowired
    private SignupService signupService;

    /** Called once the 4-step wizard (FR-T-006) is fully filled in and submitted. */
    public TenantDetails completeWizard(
            String companyName, String ownerEmail, String rawPassword,   // Step 1: identity
            String logoUrl, String primaryColor, String font,             // Step 2: branding
            String locale, String timezone, String currency,              // Step 3: regional
            String industry) {                                            // Step 4: admin/misc

        String slug = SignupService.slugify(companyName);

        SignupRequest request = SignupRequest.builder(slug, ownerEmail, rawPassword)
                .companyName(companyName)
                .plan(TenantPlan.STARTER)
                .attribute("logoUrl", logoUrl)
                .attribute("primaryColor", primaryColor)
                .attribute("font", font)
                .attribute("locale", locale)
                .attribute("timezone", timezone)
                .attribute("currency", currency)
                .attribute("industry", industry)
                .build();

        return signupService.signup(request);
    }

    /**
     * Simplified signup for the Holon SignUpPage flow: provisions a tenant from just the
     * person's name, email and password, using sensible defaults for branding/regional/industry.
     */
    public TenantDetails signupBasic(String firstName, String lastName, String ownerEmail, String rawPassword) {
        return completeWizard(
                defaultWorkspaceName(firstName, lastName, ownerEmail), ownerEmail, rawPassword,
                null, null, null,               // branding defaults
                "en-US", "UTC", "USD",          // regional defaults
                null);                           // industry
    }

    private static String defaultWorkspaceName(String firstName, String lastName, String ownerEmail) {
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        if (!fullName.isBlank()) {
            return fullName + "'s workspace";
        }
        if (ownerEmail != null && ownerEmail.contains("@")) {
            return ownerEmail.substring(0, ownerEmail.indexOf('@')) + "'s workspace";
        }
        return "My workspace";
    }
}