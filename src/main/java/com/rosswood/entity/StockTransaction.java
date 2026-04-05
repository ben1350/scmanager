package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Every stock movement in the system.
 * Direction is enforced: IN types have positive qty, OUT types have negative qty.
 * Each transaction links back to its source document for full traceability.
 */
@Entity
@Table(name = "stock_transaction", indexes = {
        @Index(name = "idx_stx_item", columnList = "item_id"),
        @Index(name = "idx_stx_date", columnList = "transaction_date"),
        @Index(name = "idx_stx_lot", columnList = "batch_code")
})
public class StockTransaction extends AuditableEntity {

    // ── When ──────────────────────────────────────────────────────────────
    @Column(name = "transaction_date", nullable = false)
    public LocalDate transactionDate;

    // ── What ──────────────────────────────────────────────────────────────
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public Item item;

    /**
     * Batch number links this stock movement to a specific production batch.
     * Format: BATCH_CODE or manually assigned for purchases.
     * Enables "which batch is this stock from?" queries.
     */
    @Column(name = "batch_code")
    public String batchCode;

    /**
     * Expiry date of this specific lot.
     * Critical for yogurt/food products — copied from ProductionBatch on output.
     */
    @Column(name = "expiry_date")
    public LocalDate expiryDate;

    // ── How much ──────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    public StockDirection direction; // IN or OUT

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    public TransactionType transactionType;

    /**
     * Always positive — direction field controls IN vs OUT.
     * Stock balance = SUM(qty WHERE direction=IN) - SUM(qty WHERE direction=OUT)
     */
    @Column(nullable = false, precision = 12, scale = 4)
    public BigDecimal quantity;

    /**
     * Cost per unit at the time of this transaction.
     * Set on: PURCHASE (supplier price), OPENING_STOCK (entered cost),
     *         SALE (item.averageCost snapshot = COGS).
     * Null for: DAMAGE, EXPIRY_WRITEOFF, ADJUSTMENT_*, PRODUCTION_OUTPUT/CONSUME.
     */
    @Column(name = "unit_cost", precision = 12, scale = 4)
    public BigDecimal unitCost;

    @Column(name = "uom_code")
    public String uomCode;

    // ── Source document links (only one will be set per transaction) ──────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id")
    public SalesInvoice salesInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_batch_id")
    public ProductionBatch productionBatch;

    // ── Notes ─────────────────────────────────────────────────────────────
    @Column(name = "reference_no")
    public String referenceNo;

    public String remarks;

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum StockDirection {
        IN, OUT
    }

    public enum TransactionType {
        // IN types
        PURCHASE,
        PRODUCTION_OUTPUT,
        RETURN_FROM_CUSTOMER,
        OPENING_STOCK,
        ADJUSTMENT_IN,

        // OUT types
        SALE,
        PRODUCTION_CONSUME,
        DAMAGE,
        EXPIRY_WRITEOFF,
        ADJUSTMENT_OUT
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /** Current stock on hand for an item (all lots combined) */
    public static BigDecimal stockOnHand(Long itemId) {
        BigDecimal in = find("item.id = ?1 and direction = ?2", itemId, StockDirection.IN)
                .stream()
                .map(t -> ((StockTransaction) t).quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal out = find("item.id = ?1 and direction = ?2", itemId, StockDirection.OUT)
                .stream()
                .map(t -> ((StockTransaction) t).quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return in.subtract(out);
    }

    /** All transactions for a specific batch number — full lot history */
    public static List<StockTransaction> findByLot(String batchCode) {
        return list("batchCode = ?1 ORDER BY transactionDate ASC", batchCode);
    }

    /** Stock expiring on or before a given date — for near-expiry alerts */
    public static List<StockTransaction> expiringBefore(LocalDate date) {
        return list("""
            direction = 'IN' AND expiryDate <= ?1
            AND quantity > (
                SELECT COALESCE(SUM(o.quantity), 0) FROM StockTransaction o
                WHERE o.item = item AND o.batchCode = batchCode AND o.direction = 'OUT'
            )
            ORDER BY expiryDate ASC
            """, date);
    }

    /** All movements traceable to a specific invoice */
    public static List<StockTransaction> findByInvoice(Long invoiceId) {
        return list("salesInvoice.id = ?1", invoiceId);
    }

    /** All movements traceable to a specific production batch */
    public static List<StockTransaction> findByBatch(Long batchId) {
        return list("productionBatch.id = ?1", batchId);
    }
}

