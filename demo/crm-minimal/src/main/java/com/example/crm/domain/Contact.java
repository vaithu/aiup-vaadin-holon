package com.example.crm.domain;

import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.beans.DataPath;
import com.holonplatform.core.beans.Identifier;

/**
 * Contact person belonging to a {@link Customer}. The foreign key is stored as a
 * plain {@code customerId} field (Holon Datastore joins are expressed via query filters).
 */
@DataPath("contact")
public class Contact {

    public static final BeanPropertySet<Contact> PROPERTIES =
            BeanPropertySet.create(Contact.class);

    @Identifier
    @DataPath("id")
    private Long id;

    @DataPath("customer_id")
    private Long customerId;

    @DataPath("first_name")
    private String firstName;

    @DataPath("last_name")
    private String lastName;

    @DataPath("email")
    private String email;

    @DataPath("phone")
    private String phone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
