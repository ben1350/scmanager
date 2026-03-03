package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_invoice_item")
public class SalesInvoiceItem extends AuditableEntity {

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public SalesInvoice invoice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uom_id")
    public ItemUom uom; // Added to track which UOM was used for this sale

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    public Item item;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal unitPrice;

    @Column(name = "line_total", precision = 12, scale = 2)
    public BigDecimal lineTotal;
}

