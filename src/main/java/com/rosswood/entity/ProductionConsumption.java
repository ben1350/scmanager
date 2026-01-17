package com.rosswood.entity;

/* =========================
PRODUCTION CONSUMPTION
========================= */

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "production_consumption")
public class ProductionConsumption extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id")
    public ProductionBatch batch;

    @ManyToOne(optional = false)
    @JoinColumn(name = "raw_item_id")
    public Item rawItem;

    @Column(name = "consumed_qty", nullable = false, precision = 12, scale = 2)
    public BigDecimal consumedQty;
}

