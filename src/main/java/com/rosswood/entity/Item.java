package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item")
public class Item extends AuditableEntity {

    @Column(name = "item_code", unique = true, nullable = false)
    public String itemCode;

    @Column(name = "item_name", nullable = false)
    public String itemName;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_type_id")
    public ItemType itemType;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "uom_id")
    public UnitOfMeasure uom;

    /**
     * Default selling price excluding VAT.
     * Pre-populated when adding invoice lines — user can override per line.
     */
    @Column(name = "selling_price_ex_vat", precision = 12, scale = 4)
    public BigDecimal sellingPriceExVat;

    /**
     * Default VAT rate for this item (e.g. 20.00 for 20%).
     * Some items may be zero-rated.
     */
    @Column(name = "vat_rate", precision = 5, scale = 2)
    public BigDecimal vatRate = BigDecimal.ZERO;

    /**
     * Weighted average cost (WAC) — recalculated automatically on every
     * purchase or opening stock entry that includes a unit cost.
     * Null until the first cost-bearing transaction is recorded.
     */
    @Column(name = "average_cost", precision = 12, scale = 4)
    public BigDecimal averageCost;

    /**
     * Default shelf life in days for finished goods.
     * Used to auto-calculate expiryDate when a production batch is finished.
     * e.g. 21 days for fresh yogurt.
     */
    @Column(name = "shelf_life_days")
    public Integer shelfLifeDays;

    @Column(name = "active_flag")
    public Boolean activeFlag = true;

    // ── Helpers ───────────────────────────────────────────────────────────

    public static Item findByCode(String code) {
        return find("UPPER(itemCode) = ?1", code.toUpperCase()).firstResult();
    }
}
