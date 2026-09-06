package com.example.crm.ui;

import com.example.crm.service.CrmLoginService;
import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.multitenant.TenantDetailsLoader;
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.Input;
import com.holonplatform.vaadin.flow.components.SingleSelect;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Optional;

@Route("login")
@PageTitle("Sign in")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final CrmLoginService loginService;
    private final TenantDetailsLoader tenantDetailsLoader;

    private final Input<String> emailField = Components.input.string().label("Email").build();
    private final Input<String> passwordField = Components.input.password().label("Password").build();
    private final Div errorMessage = new Div();
    private final Div pickerContainer = new Div();

    private SingleSelect<String> tenantPicker;
    private List<String> pendingTenantIds = List.of();

    public LoginView(CrmLoginService loginService, TenantDetailsLoader tenantDetailsLoader) {
        this.loginService = loginService;
        this.tenantDetailsLoader = tenantDetailsLoader;

        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMessage.setVisible(false);

        var signInButton = Components.button()
                .text("Sign in")
                .withClickListener(event -> submit())
                .build();

        var signUpButton = Components.button()
                .text("Create an account")
                .withClickListener(event -> UI.getCurrent().navigate("signup"))
                .build();

        add(new H1("IyenSoft CRM"),
                emailField.getComponent(),
                passwordField.getComponent(),
                errorMessage,
                pickerContainer,
                signInButton,
                signUpButton);

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
    }

    private void submit() {
        String email = emailField.getValue();
        String password = passwordField.getValue();

        if (!pendingTenantIds.isEmpty()) {
            // Step 2: the user already chose a workspace from the picker.
            String tenantId = tenantPicker.getValue();
            if (tenantId == null) {
                showError("Please choose a workspace.");
                return;
            }
            attemptLogin(tenantId, email, password);
            return;
        }

        List<String> tenantIds = loginService.findTenantsForEmail(email);
        if (tenantIds.isEmpty()) {
            showError("No account found for that email.");
        } else if (tenantIds.size() == 1) {
            attemptLogin(tenantIds.get(0), email, password);
        } else {
            showWorkspacePicker(tenantIds);
        }
    }

    private void attemptLogin(String tenantId, String email, String password) {
        Optional<TenantDetails> tenant = loginService.authenticate(tenantId, email, password);
        if (tenant.isPresent()) {
            // Full page navigation (not UI.navigate) so the fresh HTTP request carries the
            // real /t/{tenantId}/... path, letting PathPrefixTenantResolver resolve it and
            // seed the session cache for every later in-app request.
            UI.getCurrent().getPage().setLocation("/t/" + tenant.get().tenantId() + "/contacts");
        } else {
            showError("Incorrect email or password.");
        }
    }

    private void showWorkspacePicker(List<String> tenantIds) {
        pendingTenantIds = tenantIds;

        tenantPicker = Components.input.singleOptionSelect(String.class)
                .label("Choose your workspace")
                .dataSource(new ListDataProvider<>(tenantIds))
                .itemCaptionGenerator(this::displayNameFor)
                .build();

        pickerContainer.removeAll();
        pickerContainer.add(tenantPicker.getComponent());
        errorMessage.setVisible(false);
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

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }
}