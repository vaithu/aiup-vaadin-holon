package com.example.crm.ui;

import com.example.crm.service.CrmSignupHandler;
import com.holonplatform.multitenant.TenantDetails;
import com.iyensoft.vaadin.flow.components.SignUpPage;
import com.iyensoft.vaadin.flow.components.builders.SignUpPageBuilder;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("signup")
@PageTitle("Create your workspace")
@AnonymousAllowed
public class SignupView extends VerticalLayout {

    private final transient CrmSignupHandler signupHandler;

    public SignupView(CrmSignupHandler signupHandler) {
        this.signupHandler = signupHandler;

        SignUpPage signUpPage = SignUpPageBuilder.create()
                .heading("Create your workspace")
                .subtitle("Start your CRM in seconds")
                .socialLogin(false)
                .terms(true)
                .signIn(true)
                .withSignUpListener(this::onSignUp)
                .withSignInListener(event -> UI.getCurrent().navigate("login"))
                .build();

        add(signUpPage);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
    }

    private void onSignUp(SignUpPage.SignUpEvent event) {
        SignUpPage page = event.getSource();
        if (!event.isAgreedToTerms()) {
            page.setErrorMessage("Please accept the terms to continue.");
            return;
        }
        try {
            TenantDetails tenant = signupHandler.signupBasic(
                    event.getFirstName(), event.getLastName(), event.getEmail(), event.getPassword());

            Notification.show("Workspace '" + tenant.name() + "' created! Please sign in.",
                    4000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("login");
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(SignupView.class).error("Signup failed", e);
            page.setErrorMessage("Could not create the account: " + e.getMessage());
        }
    }
}