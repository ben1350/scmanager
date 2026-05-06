package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
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

    /**
     * CASH customers may only pay by CASH, MOMO or CHEQUE.
     * CREDIT customers may also pay on account (payment method = CREDIT).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    public CustomerType customerType = CustomerType.CASH;

    /**
     * Maximum allowed outstanding credit balance.
     * Only enforced when customerType == CREDIT.
     * Null means no limit configured.
     */
    @Column(name = "credit_limit", precision = 12, scale = 2)
    public BigDecimal creditLimit;

    // One Customer can have many Branches
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    public List<CustomerBranch> branches = new ArrayList<>();

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum CustomerType {
        CASH, CREDIT
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    // Helper method to sync bidirectional relationship
    public void addBranch(CustomerBranch branch) {
        branches.add(branch);
        branch.customer = this;
    }
}

