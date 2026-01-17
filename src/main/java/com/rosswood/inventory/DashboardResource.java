package com.rosswood.inventory;

import com.rosswood.entity.ProductionBatch;
import com.rosswood.entity.StockTransaction;
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

        // KPI 1: Total items in stock
        var totalStock = StockTransaction.find("select sum(quantity) from StockTransaction").project(BigDecimal.class).firstResult();
        // KPI 2: Low stock items (arbitrary threshold e.g., < 10)
        Long lowStock = StockTransaction.find("select count(distinct s.item) from StockTransaction s group by s.item having sum(s.quantity) < 10").project(Long.class).count();

        // KPI 3: Today's sales
        BigDecimal todaysSales = StockTransaction.find("select sum(quantity * 1) from StockTransaction where transactionType = ?1 and transactionDate = ?2",
                "SALE", LocalDate.now()).project(BigDecimal.class).singleResult();
        if (todaysSales == null) todaysSales = BigDecimal.ZERO;

        // KPI 4: Pending production batches
        List<ProductionBatch> pendingBatches = ProductionBatch.list("order by productionDate desc");

        // Recent stock transactions (last 5)
        List<StockTransaction> recentTransactions = StockTransaction.find("order by transactionDate desc").page(0, 5).list();

        return dashboard
                .data("totalStock", totalStock != null ? totalStock : BigDecimal.ZERO)
                .data("lowStock", lowStock)
                .data("todaysSales", todaysSales)
                .data("pendingProduction", pendingBatches.size())
                .data("recentTransactions", recentTransactions)
                .data("pendingBatches", pendingBatches.size() > 5 ? pendingBatches.subList(0, 5) : pendingBatches);
    }
}
