package com.rosswood.service;

import com.rosswood.entity.*;
import com.rosswood.entity.StockTransaction.StockDirection;
import com.rosswood.entity.StockTransaction.TransactionType;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReportService {

    // ── Stock on Hand ─────────────────────────────────────────────────────

    /**
     * Stock on hand per item with lot breakdown.
     * Returns list of StockSummary records.
     */
    public List<StockSummaryRow> stockOnHand() {
        List<Item> items = Item.list("activeFlag = true ORDER BY itemName ASC");
        return items.stream().map(item -> {
            BigDecimal qty = StockTransaction.stockOnHand(item.id);
            // Get earliest expiry date with remaining stock
            LocalDate nextExpiry = StockTransaction
                    .<StockTransaction>find(
                            "item.id = ?1 AND direction = ?2 AND expiryDate IS NOT NULL ORDER BY expiryDate ASC",
                            item.id, StockDirection.IN)
                    .firstResultOptional()
                    .map(t -> t.expiryDate)
                    .orElse(null);
            return new StockSummaryRow(item, qty, nextExpiry);
        }).collect(Collectors.toList());
    }

    /**
     * Lot-level breakdown for a specific item.
     */
    public List<LotRow> stockByLot(Long itemId) {
        // Get all IN transactions for item grouped by batchCode
        List<StockTransaction> ins = StockTransaction.list(
                "item.id = ?1 AND direction = ?2 AND batchCode IS NOT NULL ORDER BY transactionDate ASC",
                itemId, StockDirection.IN);

        return ins.stream().map(t -> {
                    // Calculate remaining qty for this lot
                    BigDecimal out = StockTransaction
                            .<StockTransaction>find(
                                    "item.id = ?1 AND direction = ?2 AND batchCode = ?3",
                                    itemId, StockDirection.OUT, t.batchCode)
                            .stream()
                            .map(tx -> tx.quantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal remaining = t.quantity.subtract(out);
                    return new LotRow(t.batchCode, t.transactionDate, t.expiryDate, t.quantity, remaining, t.uomCode);
                }).filter(r -> r.remaining.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    // ── Sales Reports ─────────────────────────────────────────────────────

    /**
     * Sales summary between two dates.
     */
    public List<SalesInvoice> salesByDateRange(LocalDate from, LocalDate to) {
        return SalesInvoice.list(
                "invoiceDate >= ?1 AND invoiceDate <= ?2 AND status != ?3 ORDER BY invoiceDate DESC",
                from, to, SalesInvoice.InvoiceStatus.CANCELLED);
    }

    /**
     * Sales by customer between two dates.
     */
    public List<CustomerSalesRow> salesByCustomer(LocalDate from, LocalDate to) {
        List<SalesInvoice> invoices = salesByDateRange(from, to);
        return invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.customerBranch.customer.name,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new CustomerSalesRow(
                                        list.get(0).customerBranch.customer,
                                        list.size(),
                                        list.stream().map(i -> i.totalAmountExVat != null ? i.totalAmountExVat : BigDecimal.ZERO)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                                        list.stream().map(i -> i.vatAmount != null ? i.vatAmount : BigDecimal.ZERO)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                                        list.stream().map(i -> i.totalAmount != null ? i.totalAmount : BigDecimal.ZERO)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                )
                        )
                ))
                .values().stream()
                .sorted((a, b) -> b.totalAmount.compareTo(a.totalAmount))
                .collect(Collectors.toList());
    }

    /**
     * Top selling items between two dates.
     */
    public List<ItemSalesRow> topSellingItems(LocalDate from, LocalDate to) {
        List<StockTransaction> sales = StockTransaction.list(
                "transactionType = ?1 AND transactionDate >= ?2 AND transactionDate <= ?3 ORDER BY transactionDate DESC",
                TransactionType.SALE, from, to);

        return sales.stream()
                .collect(Collectors.groupingBy(
                        t -> t.item.id,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new ItemSalesRow(
                                        list.get(0).item,
                                        list.stream().map(t -> t.quantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                                        list.size()
                                )
                        )
                ))
                .values().stream()
                .sorted((a, b) -> b.totalQty.compareTo(a.totalQty))
                .collect(Collectors.toList());
    }

    // ── Production Reports ────────────────────────────────────────────────

    /**
     * Production summary between two dates.
     */
    public List<ProductionBatch> productionByDateRange(LocalDate from, LocalDate to) {
        return ProductionBatch.list(
                "productionDate >= ?1 AND productionDate <= ?2 ORDER BY productionDate DESC",
                from, to);
    }

    /**
     * Raw material consumption between two dates.
     */
    public List<MaterialConsumptionRow> materialConsumption(LocalDate from, LocalDate to) {
        List<StockTransaction> consumes = StockTransaction.list(
                "transactionType = ?1 AND transactionDate >= ?2 AND transactionDate <= ?3",
                TransactionType.PRODUCTION_CONSUME, from, to);

        return consumes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.item.id,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new MaterialConsumptionRow(
                                        list.get(0).item,
                                        list.stream().map(t -> t.quantity).reduce(BigDecimal.ZERO, BigDecimal::add)
                                )
                        )
                ))
                .values().stream()
                .sorted((a, b) -> b.totalConsumed.compareTo(a.totalConsumed))
                .collect(Collectors.toList());
    }

    // ── Expiry Reports ────────────────────────────────────────────────────

    public List<ProductionBatch> expiringWithin(int days) {
        return ProductionBatch.expiringBefore(LocalDate.now().plusDays(days));
    }

    // ── Batch Traceability ────────────────────────────────────────────────

    /**
     * Full traceability for a batch: what was consumed, what was produced,
     * and which customers received stock from this batch.
     */
    public BatchTraceRow batchTrace(String batchCode) {
        ProductionBatch batch = ProductionBatch.find("batchCode = ?1", batchCode).firstResult();
        if (batch == null) return null;

        // Stock OUT transactions for this batch = what was sold from it
        List<StockTransaction> sales = StockTransaction.list(
                "batchCode = ?1 AND direction = ?2", batchCode, StockDirection.OUT);

        // Distinct invoices
        List<SalesInvoice> invoices = sales.stream()
                .filter(t -> t.salesInvoice != null)
                .map(t -> t.salesInvoice)
                .distinct()
                .collect(Collectors.toList());

        return new BatchTraceRow(batch, sales, invoices);
    }

    // ── Dashboard KPIs ────────────────────────────────────────────────────

    public DashboardKpis dashboardKpis() {
        // Total stock value (qty only — no unit cost yet)
        List<StockSummaryRow> stock = stockOnHand();
        BigDecimal totalStockQty = stock.stream()
                .map(r -> r.qty).reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStockCount = stock.stream()
                .filter(r -> r.qty.compareTo(BigDecimal.TEN) < 0).count();

        // Today's sales
        BigDecimal todaysSales = SalesInvoice
                .<SalesInvoice>find("invoiceDate = ?1 AND status != ?2",
                        LocalDate.now(), SalesInvoice.InvoiceStatus.CANCELLED)
                .stream()
                .map(i -> i.totalAmount != null ? i.totalAmount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // This month sales
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        BigDecimal monthSales = SalesInvoice
                .<SalesInvoice>find("invoiceDate >= ?1 AND status != ?2",
                        monthStart, SalesInvoice.InvoiceStatus.CANCELLED)
                .stream()
                .map(i -> i.totalAmount != null ? i.totalAmount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openBatches     = ProductionBatch.count("status = ?1", ProductionBatch.BatchStatus.OPEN);
        long expiringIn7Days = ProductionBatch.count("status = ?1 AND expiryDate <= ?2",
                ProductionBatch.BatchStatus.FINISHED, LocalDate.now().plusDays(7));
        long pendingDelivery = SalesInvoice.count("status = ?1", SalesInvoice.InvoiceStatus.CONFIRMED);

        return new DashboardKpis(totalStockQty, lowStockCount, todaysSales, monthSales,
                openBatches, expiringIn7Days, pendingDelivery);
    }

    // ── Row types (records) ───────────────────────────────────────────────

    public record StockSummaryRow(Item item, BigDecimal qty, LocalDate nextExpiry) {
        public String qtyClass() {
            if (qty.compareTo(BigDecimal.ZERO) <= 0) return "qty-zero";
            if (qty.compareTo(BigDecimal.TEN)  <  0) return "qty-low";
            return "qty-ok";
        }
        public String expiryClass(LocalDate in7, LocalDate in30) {
            if (nextExpiry == null) return "exp-ok";
            if (nextExpiry.isBefore(in7))  return "exp-urgent";
            if (nextExpiry.isBefore(in30)) return "exp-soon";
            return "exp-ok";
        }
    }
    public record LotRow(String batchCode, LocalDate productionDate, LocalDate expiryDate,
                         BigDecimal originalQty, BigDecimal remaining, String uomCode) {}
    public record CustomerSalesRow(Customer customer, int invoiceCount,
                                   BigDecimal totalExVat, BigDecimal totalVat, BigDecimal totalAmount) {}
    public record ItemSalesRow(Item item, BigDecimal totalQty, int transactionCount) {}
    public record MaterialConsumptionRow(Item item, BigDecimal totalConsumed) {}
    public record BatchTraceRow(ProductionBatch batch, List<StockTransaction> movements,
                                List<SalesInvoice> invoices) {}
    public record DashboardKpis(BigDecimal totalStockQty, long lowStockCount,
                                BigDecimal todaysSales, BigDecimal monthSales,
                                long openBatches, long expiringIn7Days, long pendingDelivery) {}
}