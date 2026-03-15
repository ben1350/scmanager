package com.rosswood.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A production run that consumes raw materials and produces finished goods.
 * The batchCode becomes the lotNumber on all resulting stock transactions,
 * enabling full forward/backward traceability.
 */
@Entity
@Table(name = "production_batch")
public class ProductionBatch extends AuditableEntity {

    @Column(name = "batch_code", unique = true, nullable = false)
    public String batchCode;

    @Column(name = "production_date", nullable = false)
    public LocalDate productionDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "finished_item_id")
    public Item finishedItem;

    @Column(name = "output_qty", precision = 12, scale = 4)
    public BigDecimal outputQty;

    /**
     * Expiry date of the finished goods produced in this batch.
     * This is propagated to StockTransaction.expiryDate when the batch is finished,
     * enabling expiry tracking at the lot level.
     */
    @Column(name = "expiry_date")
    public LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public BatchStatus status = BatchStatus.OPEN;

    public String remarks;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<ProductionConsumption> consumptions = new ArrayList<>();

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum BatchStatus {
        /** Batch created, consumptions being recorded */
        OPEN,
        /** Batch finished — stock transactions have been posted */
        FINISHED,
        /** Batch cancelled — no stock impact */
        CANCELLED
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public void addConsumption(ProductionConsumption c) {
        consumptions.add(c);
        c.batch = this;
    }

    /** Find all batches that produced a specific item */
    public static List<ProductionBatch> findByItem(Long itemId) {
        return list("finishedItem.id = ?1 ORDER BY productionDate DESC", itemId);
    }

    /** Find open batches */
    public static List<ProductionBatch> findOpen() {
        return list("status = ?1 ORDER BY productionDate DESC", BatchStatus.OPEN);
    }

    /** Find batches with stock expiring before a date */
    public static List<ProductionBatch> expiringBefore(LocalDate date) {
        return list("status = ?1 AND expiryDate <= ?2 ORDER BY expiryDate ASC",
                BatchStatus.FINISHED, date);
    }
}