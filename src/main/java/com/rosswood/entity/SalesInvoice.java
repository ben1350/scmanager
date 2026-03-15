package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A sales invoice issued to a customer branch.
 * Status lifecycle: DRAFT → CONFIRMED → DELIVERED → CANCELLED
 * Only DRAFT invoices can be edited. CONFIRMED invoices post stock transactions.
 */
@Entity
@Table(name = "sales_invoice")
public class SalesInvoice extends AuditableEntity {

    @Column(name = "rosswood_invoice_no", unique = true, nullable = false)
    public String invoiceNo;

    @Column(name = "vat_invoice_no")
    public String vatInvoiceNo;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_branch_id")
    public CustomerBranch customerBranch;

    @Column(name = "invoice_date", nullable = false)
    public LocalDate invoiceDate;

    /**
     * Actual delivery date — often different from invoice date.
     * Disputes frequently arise from this gap; tracking it closes that loop.
     */
    @Column(name = "delivery_date")
    public LocalDate deliveryDate;

    /**
     * Delivery note or waybill reference number.
     * Links the invoice to the physical delivery document.
     */
    @Column(name = "delivery_note_no")
    public String deliveryNoteNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "total_amount_ex_vat", precision = 12, scale = 2)
    public BigDecimal totalAmountExVat = BigDecimal.ZERO;

    @Column(name = "vat_amount", precision = 12, scale = 2)
    public BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    public String remarks;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<SalesInvoiceItem> items = new ArrayList<>();

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum InvoiceStatus {
        /** Being built — editable, no stock impact yet */
        DRAFT,
        /** Confirmed — stock transactions posted, no more edits */
        CONFIRMED,
        /** Goods physically delivered to customer */
        DELIVERED,
        /** Voided — reverse stock transactions posted */
        CANCELLED
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public void addItem(SalesInvoiceItem item) {
        items.add(item);
        item.invoice = this;
        recalculateTotals();
    }

    public void removeItem(SalesInvoiceItem item) {
        items.remove(item);
        item.invoice = null;
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.totalAmountExVat = items.stream()
                .map(i -> i.lineTotal != null ? i.lineTotal : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.vatAmount = items.stream()
                .map(i -> i.vatAmount != null ? i.vatAmount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = this.totalAmountExVat.add(this.vatAmount);
    }

    public boolean isEditable() {
        return this.status == InvoiceStatus.DRAFT;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public static List<SalesInvoice> findByCustomer(Long customerId) {
        return list("customerBranch.customer.id = ?1 ORDER BY invoiceDate DESC", customerId);
    }

    public static List<SalesInvoice> findByStatus(InvoiceStatus status) {
        return list("status = ?1 ORDER BY invoiceDate DESC", status);
    }

    public static List<SalesInvoice> findUndelivered() {
        return list("status = ?1 AND deliveryDate IS NULL ORDER BY invoiceDate ASC",
                InvoiceStatus.CONFIRMED);
    }
}

