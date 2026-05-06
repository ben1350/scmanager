package rest;

import com.rosswood.entity.*;
import com.rosswood.service.ReportService;
import com.rosswood.service.ReportService.*;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/reports")
public class ReportsController extends HxController {

    @Inject
    ReportService reportService;

    @CheckedTemplate(requireTypeSafeExpressions = false)
    public static class Templates {
        public static native TemplateInstance index();
        public static native TemplateInstance stockOnHand(List<StockSummaryRow> rows, LocalDate asOf,
                                                          long lowStockCount, long outOfStockCount,
                                                          LocalDate in7days, LocalDate in30days,
                                                          BigDecimal totalStockValue);
        public static native TemplateInstance stockByLot(List<LotRow> lots, Item item,
                                                         LocalDate in7days, LocalDate in30days);
        public static native TemplateInstance salesSummary(List<SalesInvoice> invoices,
                                                           List<CustomerSalesRow> byCustomer,
                                                           List<ItemSalesRow> topItems,
                                                           LocalDate from, LocalDate to,
                                                           BigDecimal grandTotal);
        public static native TemplateInstance productionSummary(List<ProductionBatch> batches,
                                                                List<MaterialConsumptionRow> consumption,
                                                                LocalDate from, LocalDate to,
                                                                long finishedCount);
        public static native TemplateInstance expiryReport(List<ProductionBatch> expiring7,
                                                           List<ProductionBatch> expiring30);
        public static native TemplateInstance batchTrace(BatchTraceRow trace, String batchCode);
    }

    @GET
    public TemplateInstance index() {
        return Templates.index();
    }

    @GET
    @Path("/stock")
    public TemplateInstance stockOnHand() {
        List<StockSummaryRow> rows = reportService.stockOnHand();
        long lowStock    = rows.stream().filter(r -> r.qty().compareTo(BigDecimal.TEN) < 0
                && r.qty().compareTo(BigDecimal.ZERO) > 0).count();
        long outOfStock  = rows.stream().filter(r -> r.qty().compareTo(BigDecimal.ZERO) <= 0).count();
        BigDecimal totalStockValue = rows.stream()
                .filter(r -> r.stockValue() != null)
                .map(StockSummaryRow::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Templates.stockOnHand(rows, LocalDate.now(), lowStock, outOfStock,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30), totalStockValue);
    }

    @GET
    @Path("/stock/{itemId}/lots")
    public TemplateInstance stockByLot(@PathParam("itemId") Long itemId) {
        Item item = Item.findById(itemId);
        if (item == null) throw new NotFoundException();
        return Templates.stockByLot(reportService.stockByLot(itemId), item,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30));
    }

    @GET
    @Path("/sales")
    public TemplateInstance salesSummary(@RestQuery String from, @RestQuery String to) {
        LocalDate dateFrom = from != null ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
        LocalDate dateTo   = to   != null ? LocalDate.parse(to)   : LocalDate.now();

        List<SalesInvoice>     invoices   = reportService.salesByDateRange(dateFrom, dateTo);
        List<CustomerSalesRow> byCustomer = reportService.salesByCustomer(dateFrom, dateTo);
        List<ItemSalesRow>     topItems   = reportService.topSellingItems(dateFrom, dateTo);

        BigDecimal grandTotal = invoices.stream()
                .map(i -> i.totalAmount != null ? i.totalAmount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Templates.salesSummary(invoices, byCustomer, topItems, dateFrom, dateTo, grandTotal);
    }

    @GET
    @Path("/production")
    public TemplateInstance productionSummary(@RestQuery String from, @RestQuery String to) {
        LocalDate dateFrom = from != null ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
        LocalDate dateTo   = to   != null ? LocalDate.parse(to)   : LocalDate.now();

        List<ProductionBatch>       batches     = reportService.productionByDateRange(dateFrom, dateTo);
        List<MaterialConsumptionRow> consumption = reportService.materialConsumption(dateFrom, dateTo);
        long finishedCount = batches.stream()
                .filter(b -> b.status == ProductionBatch.BatchStatus.FINISHED).count();

        return Templates.productionSummary(batches, consumption, dateFrom, dateTo, finishedCount);
    }

    @GET
    @Path("/expiry")
    public TemplateInstance expiryReport() {
        return Templates.expiryReport(reportService.expiringWithin(7), reportService.expiringWithin(30));
    }

    @GET
    @Path("/trace")
    public TemplateInstance batchTrace(@RestQuery String batchCode) {
        if (batchCode == null || batchCode.isBlank()) return Templates.batchTrace(null, null);
        return Templates.batchTrace(reportService.batchTrace(batchCode), batchCode);
    }
}
