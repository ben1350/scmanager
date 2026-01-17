package com.rosswood.entity;

/* =========================
STOCK OPENING
========================= */

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_opening",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_date", "item_id"}))
public class StockOpening extends AuditableEntity {

    @Column(name = "stock_date", nullable = false)
    public LocalDate stockDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    public Item item;

    @Column(name = "opening_qty", nullable = false, precision = 12, scale = 2)
    public BigDecimal openingQty;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}
