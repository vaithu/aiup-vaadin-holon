package com.example.crm;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Spring Boot entry point for the MiniCRM demo. The only Spring stereotype in the module besides
 * class is a plain constructor-injected Holon component (no {@code @Service}/{@code @Autowired}).
 * <p>
 * The Maven {@code mainClass} previously pointed at the framework's own
 * {@code com.holonplatform.multitenancy.demo.DemoApplication} (a leftover from the starter
 * template); that class component-scans its own package only and never picks up
 * {@code com.example.crm}, so it cannot boot this module. This class is the real entry point.
 */
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@EntityScan(basePackages = "com.example.crm.domain")
public class CrmApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}

}
