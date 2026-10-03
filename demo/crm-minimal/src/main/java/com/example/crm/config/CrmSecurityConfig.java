package com.example.crm.config;

import com.example.crm.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

@Configuration
@EnableWebSecurity
public class CrmSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // Public stylesheets injected via context:// into every page (incl. the anonymous login/signup views)
                .requestMatchers("/*.css").permitAll()
                .requestMatchers("/accept-invite/**", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
        );

        // Vaadin's own navigation-access-control: redirects anonymous users hitting a
        // protected view to LoginView, and handles Vaadin-internal request exclusions
        // (static resources, CSRF for UIDL, etc.) automatically.
        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                configurer.loginView(LoginView.class));

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // The Holon AuthContext consumed by the Vaadin navigator's route guards is provided by
    // tenant-security's TenantSecurityAutoConfiguration (springSecurityAuthContext bean), and
    // tenant binding from the authenticated principal on Vaadin UIDL requests is handled by its
    // AuthenticatedPrincipalTenantContextFilter — both enabled by default.
}