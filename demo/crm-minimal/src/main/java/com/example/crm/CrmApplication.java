package com.example.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MiniCRM entry point. {@code @SpringBootApplication} is the single permitted Spring
 * stereotype — everything else is wired through Holon starters and the Holon Context.
 */
@SpringBootApplication
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
