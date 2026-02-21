package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_invoice")
public class SalesInvoice extends AuditableEntity {

    @Column(name = "rosswood_invoice_no", unique = true, nullable = false)
    public String invoiceNo;

    @Column(name="vat_invoice_no", nullable=true)
    public String vatInvoiceNo;

    // Change this from Customer to CustomerBranch
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_branch_id")
    public CustomerBranch customerBranch;  //in banking parlance this will be the acc_no

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<SalesInvoiceItem> items = new ArrayList<>();

    @Column(name = "invoice_date", nullable = false)
    public LocalDate invoiceDate;

    @Column(name = "total_amount", precision = 12, scale = 2)
    public BigDecimal totalAmount;

    public String remarks;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Helper method to sync bidirectional relationship.
     * This mirrors the addBranch pattern in Customer.java
     */
    public void addItem(SalesInvoiceItem item) {
        items.add(item);
        item.invoice = this;
        if (this.totalAmount == null){
            this.totalAmount =BigDecimal.ZERO;
        }
        this.totalAmount = this.totalAmount.add(item.lineTotal);
    }


}

