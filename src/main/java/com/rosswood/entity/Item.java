package com.rosswood.entity;

/* =========================
ITEM (RAW MATERIALS + FINISHED GOODS)
========================= */

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "item")
public class Item extends AuditableEntity {

    @Column(name = "item_code", unique = true, nullable = false)
    public String itemCode;

    @Column(name = "item_name", nullable = false)
    public String itemName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_type_id")
    public ItemType itemType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uom_id")
    public UnitOfMeasure uom;

    @Column(name = "active_flag")
    public Boolean activeFlag = true;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}
