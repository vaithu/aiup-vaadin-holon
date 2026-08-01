package com.example.crm.domain;

import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.beans.DataPath;
import com.holonplatform.core.beans.Identifier;

import java.time.LocalDateTime;

/**
 * Customer company. Plain JavaBean mapped with Holon bean annotations —
 * never a PropertyBox. The {@link #PROPERTIES} BeanPropertySet is the single
 * property descriptor used for Datastore queries and Vaadin bindings.
 */
@DataPath("customer")
public class Customer {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    public static final BeanPropertySet<Customer> PROPERTIES =
            BeanPropertySet.create(Customer.class);

    @Identifier
    @DataPath("id")
    private Long id;

    @DataPath("name")
    private String name;

    @DataPath("industry")
    private String industry;

    @DataPath("status")
    private String status;

    @DataPath("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
