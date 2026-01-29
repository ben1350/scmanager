package com.rosswood.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

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

    // One Customer can have many Branches
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    public List<CustomerBranch> branches = new ArrayList<>();

    // Helper method to sync bidirectional relationship
    public void addBranch(CustomerBranch branch) {
        branches.add(branch);
        branch.customer = this;
    }
}

