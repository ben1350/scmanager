package com.rosswood.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "item_uom")
public class ItemUom extends AuditableEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    public Item item;

    @Column(name = "uom_code", nullable = false)
    public String uomCode; // e.g., "PCS", "CARTON"

    @Column(name = "is_base", nullable = false)
    public boolean isBase = false;
}
