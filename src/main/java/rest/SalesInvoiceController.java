package rest;

import com.rosswood.entity.*;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Path("/invoices")
public class SalesInvoiceController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate // Add this
    public static class Templates {
        public static native TemplateInstance index(List<SalesInvoice> invoices, List<CustomerBranch> branches, int page, int totalPages,String now);
        public static native TemplateInstance index$rows(List<SalesInvoice> invoices, int page, int totalPages);
        public static native TemplateInstance invoiceDetail(SalesInvoice invoice,List<Item> availableItems);
    }

    @GET
    public TemplateInstance index(@RestQuery Integer page) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        List<CustomerBranch> branches = CustomerBranch.listAll(); // For the "New Invoice" dropdown
        // Format the date here
        String today = LocalDate.now().toString();
        return render(currentPage, isHxRequest(), branches,today);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm @NotBlank String invoiceNo,
            @RestForm Long branchId,
            @RestForm String invoiceDate,
            @RestForm String remarks,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        SalesInvoice invoice = new SalesInvoice();
        invoice.invoiceNo = invoiceNo;
        invoice.customerBranch = CustomerBranch.findById(branchId);
        invoice.invoiceDate = LocalDate.parse(invoiceDate);
        invoice.remarks = remarks;
        invoice.persistAndFlush();
        // Format the date here
        String today = LocalDate.now().toString();

        return render(Optional.ofNullable(page).orElse(1), true, null,today);
    }

    @GET
    @Path("/{id}/items")
    public TemplateInstance getItems(@RestPath Long id) {
        onlyHxRequest();
        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();

        // Fetch all active items for the dropdown
        List<Item> availableItems = Item.list("activeFlag", true);

        return Templates.invoiceDetail(invoice, availableItems);
    }

    @POST
    @Path("/{id}/items")
    @Transactional
    public TemplateInstance addInvoiceItem(
            @RestPath Long id, @RestForm @NotNull Long itemId,
            @RestForm @NotNull Long quantity,
            @RestForm @NotNull Long unitPrice
    ) {
        onlyHxRequest();

        SalesInvoice salesInvoice = SalesInvoice.findById(id);
        var salesInvoiceItem = new SalesInvoiceItem();
        salesInvoiceItem.item = Item.findById(itemId);
        salesInvoiceItem.quantity = BigDecimal.valueOf(quantity);
        salesInvoiceItem.unitPrice = BigDecimal.valueOf(unitPrice);
        salesInvoiceItem.lineTotal = BigDecimal.valueOf(quantity *(unitPrice));
        salesInvoice.addItem(salesInvoiceItem);
        salesInvoiceItem.invoice=salesInvoice;
        salesInvoice.persist();

        return Templates.invoiceDetail(salesInvoice,Item.listAll());


    }


    private TemplateInstance render(int page, boolean fragmentOnly, List<CustomerBranch> branches,String today) {
        long totalCount = SalesInvoice.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<SalesInvoice> invoices = SalesInvoice.find("order by invoiceDate desc, id desc")
                .page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(invoices, currentPage, totalPages)
                : Templates.index(invoices, branches, currentPage, totalPages,today);
    }
}
