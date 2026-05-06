package com.rosswood.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_branch")
public class CustomerBranch extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    public Customer customer;

    @Column(name = "branch_name", nullable = false)
    public String branchName;

    public String branchAddress;

    public String contactPerson;

    public String contactPhone;

    @Column(name = "active_flag")
    public Boolean activeFlag = true;
}
