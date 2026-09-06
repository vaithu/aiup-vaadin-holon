package com.example.crm.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * Empty placeholder for the application root ("/"). Deliberately has no
 * @AnonymousAllowed — Vaadin's navigation access control treats it as requiring
 * authentication, so anonymous visitors are redirected straight to LoginView
 * (registered via VaadinSecurityConfigurer.loginView(...)). This gives the
 * "default page is the login view" behavior without mounting LoginView itself
 * at "/", which would collide with Spring Security's login-processing URL.
 */
@Route("")
public class RootView extends Div {
}