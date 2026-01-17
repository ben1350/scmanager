package com.rosswood.entity;

/* =========================
STOCK TRANSACTION
========================= */

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transaction")
public class StockTransaction extends AuditableEntity {

    @Column(name = "transaction_date", nullable = false)
    public LocalDate transactionDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    public Item item;

    @Column(name = "transaction_type", nullable = false)
    public String transactionType; // PURCHASE, PRODUCTION_CONSUME, PRODUCTION_OUTPUT, DAMAGE, SALE

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal quantity;

    @Column(name = "reference_no")
    public String referenceNo;

    public String remarks;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}

