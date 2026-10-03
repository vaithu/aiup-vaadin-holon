package com.example.crm.ui;

import com.example.crm.service.CrmLoginService;
import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.multitenant.TenantDetailsLoader;
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.SingleSelect;
import com.iyensoft.vaadin.flow.components.SignInPage;
import com.iyensoft.vaadin.flow.components.builders.SignInPageBuilder;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Optional;

@Route("login")
@PageTitle("Sign in")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final transient CrmLoginService loginService;
    private final transient TenantDetailsLoader tenantDetailsLoader;

    public LoginView(CrmLoginService loginService, TenantDetailsLoader tenantDetailsLoader) {
        this.loginService = loginService;
        this.tenantDetailsLoader = tenantDetailsLoader;

        SignInPage signInPage = SignInPageBuilder.create()
                .heading("IyenSoft CRM")
                .subtitle("Sign in to your workspace")
                .socialLogin(false)
                .keepLoggedIn(false)
                .forgotPassword(false)
                .signUp(true)
                .withSignInListener(this::onSignIn)
                .withSignUpListener(event -> UI.getCurrent().navigate("signup"))
                .build();

        add(signInPage);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
    }

    private void onSignIn(SignInPage.SignInEvent event) {
        SignInPage page = event.getSource();
        String email = event.getEmail();
        String password = event.getPassword();

        List<String> tenantIds = loginService.findTenantsForEmail(email);
        if (tenantIds.isEmpty()) {
            page.setErrorMessage("No account found for that email.");
        } else if (tenantIds.size() == 1) {
            attemptLogin(page, tenantIds.get(0), email, password);
        } else {
            showWorkspacePicker(page, tenantIds, email, password);
        }
    }

    private void attemptLogin(SignInPage page, String tenantId, String email, String password) {
        Optional<TenantDetails> tenant = loginService.authenticate(tenantId, email, password);
        if (tenant.isPresent()) {
            // Full page navigation (not UI.navigate) so the fresh HTTP request reloads the saved
            // Spring SecurityContext. The tenant is resolved from the session cache seeded during
            // authenticate() (SessionCachingTenantResolver), so a plain route — which is what
            // Vaadin's router can actually match — is used instead of a /t/{tenantId}/ prefix.
            UI.getCurrent().getPage().setLocation("/customers");
        } else {
            page.setErrorMessage("Incorrect email or password.");
        }
    }

    private void showWorkspacePicker(SignInPage page, List<String> tenantIds, String email, String password) {
        SingleSelect<String> tenantPicker = Components.input.singleOptionSelect(String.class)
                .label("Choose your workspace")
                .dataSource(new ListDataProvider<>(tenantIds))
                .itemCaptionGenerator(this::displayNameFor)
                .build();

        Components.alertDialog()
                .title("Choose your workspace")
                .bodyContent(tenantPicker.getComponent())
                .confirmText("Continue")
                .cancelText("Cancel")
                .onConfirm(() -> {
                    String tenantId = tenantPicker.getValue();
                    if (tenantId == null) {
                        return false; // keep the dialog open until a workspace is chosen
                    }
                    attemptLogin(page, tenantId, email, password);
                    return true;
                })
                .open();
    }

    private String displayNameFor(String tenantId) {
        try {
            TenantDetails details = tenantDetailsLoader.load(tenantId);
            String name = details.name();
            return (name != null && !name.isBlank()) ? name : tenantId;
        } catch (RuntimeException e) {
            return tenantId;
        }
    }
}