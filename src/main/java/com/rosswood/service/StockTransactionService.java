package com.rosswood.service;

import com.rosswood.entity.*;
import com.rosswood.entity.StockTransaction.StockDirection;
import com.rosswood.entity.StockTransaction.TransactionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Single entry point for ALL stock movements.
 * Every transaction type is handled here — no ad-hoc StockTransaction
 * creation elsewhere in the codebase.
 */
@ApplicationScoped
public class StockTransactionService {

    // ── PRODUCTION ────────────────────────────────────────────────────────

    /**
     * Post finished good output when a batch is finished.
     * Copies batchCode and expiryDate onto the transaction for traceability.
     * unitCost is the rolled-up production cost per unit (total raw material cost / output qty).
     * If provided, the finished item's WAC is recalculated.
     */
    @Transactional
    public StockTransaction postProductionOutput(ProductionBatch batch, BigDecimal qty, BigDecimal unitCost) {
        StockTransaction tx = new StockTransaction();
        tx.item             = batch.finishedItem;
        tx.quantity         = qty;
        tx.direction        = StockDirection.IN;
        tx.transactionType  = TransactionType.PRODUCTION_OUTPUT;
        tx.transactionDate  = LocalDate.now();
        tx.productionBatch  = batch;
        tx.batchCode        = batch.batchCode;
        tx.expiryDate       = batch.expiryDate;
        tx.uomCode          = uomOf(batch.finishedItem);
        tx.referenceNo      = batch.batchCode;
        tx.unitCost         = unitCost;
        tx.persist();

        if (unitCost != null) {
            recalculateWac(batch.finishedItem, qty, unitCost);
        }
        return tx;
    }

    /**
     * Post raw material consumption for each line in a batch.
     */
    @Transactional
    public StockTransaction postProductionConsume(ProductionBatch batch, ProductionConsumption consumption) {
        StockTransaction tx = new StockTransaction();
        tx.item             = consumption.rawItem;
        tx.quantity         = consumption.consumedQty; // always positive
        tx.direction        = StockDirection.OUT;
        tx.transactionType  = TransactionType.PRODUCTION_CONSUME;
        tx.transactionDate  = LocalDate.now();
        tx.productionBatch  = batch;
        tx.batchCode        = batch.batchCode;
        tx.uomCode          = uomOf(consumption.rawItem);
        tx.referenceNo      = batch.batchCode;
        tx.persist();
        return tx;
    }

    // ── SALES ─────────────────────────────────────────────────────────────

    /**
     * Post stock OUT for each line item when an invoice is confirmed.
     * batchCode on the invoice line links the sale back to the source batch.
     * unitCost is stamped with the item's current WAC as a COGS snapshot.
     */
    @Transactional
    public StockTransaction postSale(SalesInvoice invoice, SalesInvoiceItem line) {
        StockTransaction tx = new StockTransaction();
        tx.item             = line.item;
        tx.quantity         = line.quantity; // always positive
        tx.direction        = StockDirection.OUT;
        tx.transactionType  = TransactionType.SALE;
        tx.transactionDate  = invoice.invoiceDate;
        tx.salesInvoice     = invoice;
        tx.batchCode        = line.batchCode;
        tx.uomCode          = line.uom != null ? line.uom.uomCode : uomOf(line.item);
        tx.referenceNo      = invoice.invoiceNo;
        tx.unitCost         = line.item.averageCost; // COGS snapshot at time of sale
        tx.persist();
        return tx;
    }

    /**
     * Reverse a sale — post stock back IN when an invoice is cancelled.
     */
    @Transactional
    public StockTransaction reverseSale(SalesInvoice invoice, SalesInvoiceItem line) {
        StockTransaction tx = new StockTransaction();
        tx.item             = line.item;
        tx.quantity         = line.quantity;
        tx.direction        = StockDirection.IN;
        tx.transactionType  = TransactionType.RETURN_FROM_CUSTOMER;
        tx.transactionDate  = LocalDate.now();
        tx.salesInvoice     = invoice;
        tx.batchCode        = line.batchCode;
        tx.uomCode          = line.uom != null ? line.uom.uomCode : uomOf(line.item);
        tx.referenceNo      = invoice.invoiceNo;
        tx.remarks          = "Reversal of cancelled invoice " + invoice.invoiceNo;
        tx.persist();
        return tx;
    }

    // ── PURCHASES ─────────────────────────────────────────────────────────

    /**
     * Post stock IN for a purchase (raw materials arriving).
     * If unitCost is provided, the item's weighted average cost is recalculated.
     */
    @Transactional
    public StockTransaction postPurchase(Item item, BigDecimal qty, String uomCode,
                                         String referenceNo, LocalDate date, String remarks,
                                         BigDecimal unitCost) {
        StockTransaction tx = new StockTransaction();
        tx.item             = item;
        tx.quantity         = qty;
        tx.direction        = StockDirection.IN;
        tx.transactionType  = TransactionType.PURCHASE;
        tx.transactionDate  = date != null ? date : LocalDate.now();
        tx.uomCode          = uomCode != null ? uomCode : uomOf(item);
        tx.referenceNo      = referenceNo;
        tx.remarks          = remarks;
        tx.unitCost         = unitCost;
        tx.persist();

        if (unitCost != null) {
            recalculateWac(item, qty, unitCost);
        }
        return tx;
    }

    // ── OPENING STOCK ─────────────────────────────────────────────────────

    /**
     * Post opening stock balance for an item on a given date.
     * If StockOpening.unitCost is set, seeds/updates the item's WAC.
     */
    @Transactional
    public StockTransaction postOpeningStock(StockOpening opening) {
        StockTransaction tx = new StockTransaction();
        tx.item             = opening.item;
        tx.quantity         = opening.openingQty;
        tx.direction        = StockDirection.IN;
        tx.transactionType  = TransactionType.OPENING_STOCK;
        tx.transactionDate  = opening.stockDate;
        tx.uomCode          = uomOf(opening.item);
        tx.referenceNo      = "OPENING-" + opening.stockDate;
        tx.remarks          = "Opening stock entry";
        tx.unitCost         = opening.unitCost;
        tx.persist();

        if (opening.unitCost != null) {
            recalculateWac(opening.item, opening.openingQty, opening.unitCost);
        }
        return tx;
    }

    // ── DAMAGE / WRITE-OFF ────────────────────────────────────────────────

    /**
     * Write off damaged stock.
     */
    @Transactional
    public StockTransaction postDamage(Item item, BigDecimal qty, String batchCode,
                                       String referenceNo, String remarks) {
        StockTransaction tx = new StockTransaction();
        tx.item             = item;
        tx.quantity         = qty;
        tx.direction        = StockDirection.OUT;
        tx.transactionType  = TransactionType.DAMAGE;
        tx.transactionDate  = LocalDate.now();
        tx.batchCode        = batchCode;
        tx.uomCode          = uomOf(item);
        tx.referenceNo      = referenceNo;
        tx.remarks          = remarks;
        tx.persist();
        return tx;
    }

    /**
     * Write off expired stock — typically triggered by near-expiry report action.
     */
    @Transactional
    public StockTransaction postExpiryWriteOff(Item item, BigDecimal qty, String batchCode,
                                               LocalDate expiryDate, String remarks) {
        StockTransaction tx = new StockTransaction();
        tx.item             = item;
        tx.quantity         = qty;
        tx.direction        = StockDirection.OUT;
        tx.transactionType  = TransactionType.EXPIRY_WRITEOFF;
        tx.transactionDate  = LocalDate.now();
        tx.batchCode        = batchCode;
        tx.expiryDate       = expiryDate;
        tx.uomCode          = uomOf(item);
        tx.referenceNo      = "EXPIRY-" + batchCode;
        tx.remarks          = remarks;
        tx.persist();
        return tx;
    }

    // ── ADJUSTMENTS ───────────────────────────────────────────────────────

    /**
     * Manual stock adjustment IN (e.g. after stocktake variance found more stock).
     */
    @Transactional
    public StockTransaction postAdjustmentIn(Item item, BigDecimal qty, String referenceNo, String remarks) {
        StockTransaction tx = new StockTransaction();
        tx.item             = item;
        tx.quantity         = qty;
        tx.direction        = StockDirection.IN;
        tx.transactionType  = TransactionType.ADJUSTMENT_IN;
        tx.transactionDate  = LocalDate.now();
        tx.uomCode          = uomOf(item);
        tx.referenceNo      = referenceNo;
        tx.remarks          = remarks;
        tx.persist();
        return tx;
    }

    /**
     * Manual stock adjustment OUT (e.g. after stocktake variance found less stock).
     */
    @Transactional
    public StockTransaction postAdjustmentOut(Item item, BigDecimal qty, String referenceNo, String remarks) {
        StockTransaction tx = new StockTransaction();
        tx.item             = item;
        tx.quantity         = qty;
        tx.direction        = StockDirection.OUT;
        tx.transactionType  = TransactionType.ADJUSTMENT_OUT;
        tx.transactionDate  = LocalDate.now();
        tx.uomCode          = uomOf(item);
        tx.referenceNo      = referenceNo;
        tx.remarks          = remarks;
        tx.persist();
        return tx;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Recalculate and persist the weighted average cost on an item.
     *
     * Formula: newWAC = (priorQty × priorWAC + incomingQty × incomingCost)
     *                   / (priorQty + incomingQty)
     *
     * Called AFTER tx.persist(), so stockOnHand() already includes the new IN row.
     * We subtract incomingQty to recover the balance that existed beforehand.
     *
     * Edge cases:
     *   - priorQty <= 0 or item.averageCost == null → use incomingCost directly
     */
    private void recalculateWac(Item item, BigDecimal incomingQty, BigDecimal incomingCost) {
        BigDecimal totalAfter = StockTransaction.stockOnHand(item.id);
        BigDecimal priorQty   = totalAfter.subtract(incomingQty);

        BigDecimal newWac;
        if (priorQty.compareTo(BigDecimal.ZERO) <= 0 || item.averageCost == null) {
            newWac = incomingCost;
        } else {
            BigDecimal numerator   = priorQty.multiply(item.averageCost)
                                             .add(incomingQty.multiply(incomingCost));
            BigDecimal denominator = priorQty.add(incomingQty);
            newWac = numerator.divide(denominator, 4, RoundingMode.HALF_UP);
        }
        item.averageCost = newWac;
        item.persist();
    }

    private String uomOf(Item item) {
        return (item != null && item.uom != null) ? item.uom.uomCode : null;
    }
}
