package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_invoice")
public class SalesInvoice extends AuditableEntity {

    @Column(name = "invoice_no", unique = true, nullable = false)
    public String invoiceNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    public Customer customer;

    @Column(name = "invoice_date", nullable = false)
    public LocalDate invoiceDate;

    @Column(name = "total_amount", precision = 12, scale = 2)
    public BigDecimal totalAmount;

    public String remarks;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}

