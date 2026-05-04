package com.rosswood.inventory;

import com.rosswood.entity.*;
import com.rosswood.entity.SalesInvoice.InvoiceStatus;
import com.rosswood.entity.SalesInvoice.PaymentMethod;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Path("/d")
public class DashboardResource {

    @Inject
    @Location("pub/dashboard")
    Template dashboard;

    @GET
    public TemplateInstance showDashboard() {

        // KPI 1: Finished goods stock value (GHS) = sum of stockOnHand × WAC per finished good item
        List<Item> finishedGoods = Item.list("itemType.itemTypeCode", "FINISHED_GOOD");
        BigDecimal finishedGoodsValue = finishedGoods.stream()
                .filter(item -> item.averageCost != null)
                .map(item -> {
                    BigDecimal soh = StockTransaction.stockOnHand(item.id);
                    return soh.multiply(item.averageCost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // KPI 2: Low stock — items where reorderLevel is set and stockOnHand < reorderLevel
        long lowStock = Item.<Item>listAll().stream()
                .filter(item -> item.reorderLevel != null
                        && item.reorderLevel.compareTo(BigDecimal.ZERO) > 0)
                .filter(item -> StockTransaction.stockOnHand(item.id)
                        .compareTo(item.reorderLevel) < 0)
                .count();

        // KPI 3: Today's revenue (GHS) from confirmed/delivered invoices
        BigDecimal todaysRevenue = SalesInvoice
                .find("select coalesce(sum(s.totalAmount), 0) from SalesInvoice s " +
                              "where s.invoiceDate = ?1 and (s.status = ?2 or s.status = ?3)",
                        LocalDate.now(), InvoiceStatus.CONFIRMED, InvoiceStatus.DELIVERED)
                .project(BigDecimal.class).firstResult();
        if (todaysRevenue == null) todaysRevenue = BigDecimal.ZERO;
        todaysRevenue = todaysRevenue.setScale(2, RoundingMode.HALF_UP);

        // KPI 4: Pending deliveries — confirmed invoices not yet delivered
        long pendingDeliveries = SalesInvoice.count("status = ?1", InvoiceStatus.CONFIRMED);

        // KPI 5: Outstanding credit balance across all customers
        BigDecimal outstandingCredit = SalesInvoice
                .find("select coalesce(sum(s.totalAmount), 0) from SalesInvoice s " +
                              "where s.paymentMethod = ?1 and (s.status = ?2 or s.status = ?3)",
                        PaymentMethod.CREDIT, InvoiceStatus.CONFIRMED, InvoiceStatus.DELIVERED)
                .project(BigDecimal.class).firstResult();
        if (outstandingCredit == null) outstandingCredit = BigDecimal.ZERO;
        outstandingCredit = outstandingCredit.setScale(2, RoundingMode.HALF_UP);

        // Open production batches (for queue widget)
        List<ProductionBatch> openBatches = ProductionBatch.findOpen();
        List<ProductionBatch> pendingBatches = openBatches.size() > 5
                ? openBatches.subList(0, 5) : openBatches;

        // Expiring within 7 days
        List<ProductionBatch> expiringBatches = ProductionBatch
                .expiringBefore(LocalDate.now().plusDays(7));

        // Recent transactions (last 5)
        List<StockTransaction> recentTransactions = StockTransaction
                .find("order by transactionDate desc, id desc")
                .page(0, 5).list();

        return dashboard
                .data("finishedGoodsValue",  finishedGoodsValue)
                .data("lowStock",             lowStock)
                .data("todaysRevenue",        todaysRevenue)
                .data("pendingDeliveries",    pendingDeliveries)
                .data("outstandingCredit",    outstandingCredit)
                .data("recentTransactions",   recentTransactions)
                .data("pendingBatches",       pendingBatches)
                .data("expiringCount",        expiringBatches.size())
                .data("expiringBatches",      expiringBatches);
    }
}
