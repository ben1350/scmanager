package com.rosswood.inventory;

import com.rosswood.entity.ProductionBatch;
import com.rosswood.entity.StockTransaction;
import com.rosswood.entity.StockTransaction.StockDirection;
import com.rosswood.entity.StockTransaction.TransactionType;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/d")
public class DashboardResource {

    @Inject
    @Location("pub/dashboard")
    Template dashboard;

    @GET
    public TemplateInstance showDashboard() {

        // KPI 1: Total stock on hand (IN - OUT)
        BigDecimal totalIn = StockTransaction
                .find("select coalesce(sum(s.quantity), 0) from StockTransaction s where s.direction = ?1",
                        StockDirection.IN)
                .project(BigDecimal.class).firstResult();
        BigDecimal totalOut = StockTransaction
                .find("select coalesce(sum(s.quantity), 0) from StockTransaction s where s.direction = ?1",
                        StockDirection.OUT)
                .project(BigDecimal.class).firstResult();
        BigDecimal totalStock = (totalIn != null ? totalIn : BigDecimal.ZERO)
                .subtract(totalOut != null ? totalOut : BigDecimal.ZERO);

        // KPI 2: Low stock items (net stock < 10)
        long lowStock = StockTransaction.<StockTransaction>findAll().stream()
                .map(t -> t.item.id)
                .distinct()
                .filter(itemId -> StockTransaction.stockOnHand(itemId)
                        .compareTo(BigDecimal.TEN) < 0)
                .count();

        // KPI 3: Today's sales qty
        BigDecimal todaysSales = StockTransaction
                .find("select coalesce(sum(s.quantity), 0) from StockTransaction s " +
                                "where s.transactionType = ?1 and s.transactionDate = ?2",
                        TransactionType.SALE, LocalDate.now())
                .project(BigDecimal.class).firstResult();
        if (todaysSales == null) todaysSales = BigDecimal.ZERO;

        // KPI 4: Open (pending) production batches
        List<ProductionBatch> openBatches = ProductionBatch.findOpen();

        // KPI 5: Expiring within 7 days
        List<ProductionBatch> expiringBatches = ProductionBatch
                .expiringBefore(LocalDate.now().plusDays(7));

        // Recent transactions (last 5)
        List<StockTransaction> recentTransactions = StockTransaction
                .find("order by transactionDate desc, id desc")
                .page(0, 5).list();

        // Pending batches capped at 5 for the queue widget
        List<ProductionBatch> pendingBatches = openBatches.size() > 5
                ? openBatches.subList(0, 5) : openBatches;

        return dashboard
                // KPI keys — match template exactly
                .data("totalStock",          totalStock)
                .data("lowStock",            lowStock)
                .data("todaysSales",         todaysSales)
                .data("pendingProduction",   openBatches.size())
                // Lists
                .data("recentTransactions",  recentTransactions)
                .data("pendingBatches",      pendingBatches)
                // Extras for future template use
                .data("expiringCount",       expiringBatches.size())
                .data("expiringBatches",     expiringBatches);
    }
}
