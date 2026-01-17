package com.rosswood.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer extends AuditableEntity {

    @Column(name = "customer_code", unique = true, nullable = false)
    public String customerCode;

    @Column(nullable = false)
    public String name;

    public String address;

    public String email;

    public String phone;
}

