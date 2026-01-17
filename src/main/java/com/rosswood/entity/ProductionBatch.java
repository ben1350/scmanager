package com.rosswood.entity;

/* =========================
PRODUCTION BATCH
========================= */

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_batch")
public class ProductionBatch extends AuditableEntity {

    @Column(name = "batch_code", unique = true, nullable = false)
    public String batchCode;

    @Column(name = "production_date", nullable = false)
    public LocalDate productionDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "finished_item_id")
    public Item finishedItem;

    @Column(name = "output_qty", nullable = false, precision = 12, scale = 2)
    public BigDecimal outputQty;

    public String remarks;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}
