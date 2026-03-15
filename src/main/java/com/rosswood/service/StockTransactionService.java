package com.rosswood.service;

import com.rosswood.entity.*;
import com.rosswood.entity.StockTransaction.StockDirection;
import com.rosswood.entity.StockTransaction.TransactionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
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
     */
    @Transactional
    public StockTransaction postProductionOutput(ProductionBatch batch, BigDecimal qty) {
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
        tx.persist();
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
     */
    @Transactional
    public StockTransaction postPurchase(Item item, BigDecimal qty, String uomCode,
                                         String referenceNo, LocalDate date, String remarks) {
        StockTransaction tx = new StockTransaction();
        tx.item             = item;
        tx.quantity         = qty;
        tx.direction        = StockDirection.IN;
        tx.transactionType  = TransactionType.PURCHASE;
        tx.transactionDate  = date != null ? date : LocalDate.now();
        tx.uomCode          = uomCode != null ? uomCode : uomOf(item);
        tx.referenceNo      = referenceNo;
        tx.remarks          = remarks;
        tx.persist();
        return tx;
    }

    // ── OPENING STOCK ─────────────────────────────────────────────────────

    /**
     * Post opening stock balance for an item on a given date.
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
        tx.persist();
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

    // ── Private helper ────────────────────────────────────────────────────

    private String uomOf(Item item) {
        return (item != null && item.uom != null) ? item.uom.uomCode : null;
    }
}
