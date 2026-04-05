package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "sales_invoice_item")
public class SalesInvoiceItem extends AuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public SalesInvoice invoice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public Item item;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    public ItemUom uom;

    /**
     * Batch number of stock being sold — links this sale back to a production batch.
     * Enables: "which batch did we sell to this customer?"
     */
    @Column(name = "batch_code")
    public String batchCode;

    @Column(nullable = false, precision = 12, scale = 4)
    public BigDecimal quantity;

    @Column(name = "unit_price_ex_vat", nullable = false, precision = 12, scale = 4)
    public BigDecimal unitPriceExVat;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    public BigDecimal vatRate = BigDecimal.ZERO; // e.g. 20.00 for 20%

    @Column(name = "line_total_ex_vat", precision = 12, scale = 2)
    public BigDecimal lineTotal; // ex-VAT line total

    @Column(name = "vat_amount", precision = 12, scale = 2)
    public BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "line_total_inc_vat", precision = 12, scale = 2)
    public BigDecimal lineTotalIncVat;

    /**
     * Line-level discount percentage (e.g. 10.00 for 10%).
     * Applied to unitPriceExVat before calculating the line total.
     * Null / zero means no discount.
     */
    @Column(name = "discount_pct", precision = 5, scale = 2)
    public BigDecimal discountPct;

    // ── Helper ────────────────────────────────────────────────────────────

    public void calculate() {
        BigDecimal effectivePrice = this.unitPriceExVat;
        if (this.discountPct != null && this.discountPct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = effectivePrice
                    .multiply(this.discountPct)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            effectivePrice = effectivePrice.subtract(discountAmount);
        }

        this.lineTotal = effectivePrice
                .multiply(this.quantity)
                .setScale(2, RoundingMode.HALF_UP);

        if (this.vatRate != null && this.vatRate.compareTo(BigDecimal.ZERO) > 0) {
            this.vatAmount = this.lineTotal
                    .multiply(this.vatRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            this.vatAmount = BigDecimal.ZERO;
        }

        this.lineTotalIncVat = this.lineTotal.add(this.vatAmount);
    }
}

