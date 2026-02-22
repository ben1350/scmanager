package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_uom_conversion")
public class ItemUomConversion extends AuditableEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    public Item item;

    @Column(name = "from_uom", nullable = false)
    public String fromUom; // e.g., "CARTON"

    @Column(name = "to_uom", nullable = false)
    public String toUom;   // e.g., "PCS"

    @Column(name = "conversion_factor", precision = 12, scale = 4)
    public BigDecimal conversionFactor; // e.g., 12.00
}
