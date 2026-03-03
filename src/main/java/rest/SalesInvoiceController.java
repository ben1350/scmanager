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

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<SalesInvoice> invoices, List<CustomerBranch> branches, int page, int totalPages, String now);
        public static native TemplateInstance index$rows(List<SalesInvoice> invoices, int page, int totalPages);
        public static native TemplateInstance invoiceFormFragment(List<CustomerBranch> branches);
        public static native TemplateInstance invoiceDetail(SalesInvoice invoice, List<Item> availableItems);
        // Fragment for dynamic UOM loading
        public static native TemplateInstance uomOptions(List<ItemUom> uoms);
    }

    @GET
    @Path("/form-fragment")
    public TemplateInstance getFormFragment() {
        onlyHxRequest();
        return Templates.invoiceFormFragment(CustomerBranch.listAll());
    }

    @GET
    public TemplateInstance index(@RestQuery Integer page) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        List<CustomerBranch> branches = CustomerBranch.listAll();
        String today = LocalDate.now().toString();
        return render(currentPage, isHxRequest(), branches, today);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm String vatInvoiceNo,
            @RestForm Long branchId,
            @RestForm String invoiceDate,
            @RestForm String remarks,
            @RestForm Integer page
    ) {
        onlyHxRequest();
        SalesInvoice invoice = new SalesInvoice();
        invoice.vatInvoiceNo = vatInvoiceNo;
        long count = SalesInvoice.count();
        invoice.invoiceNo = "RW-INV-" + String.format("%05d", count + 1);
        invoice.customerBranch = CustomerBranch.findById(branchId);
        invoice.invoiceDate = LocalDate.parse(invoiceDate);
        invoice.remarks = remarks;
        invoice.persistAndFlush();
        return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString());
    }

    @GET
    @Path("/{id}/items")
    public TemplateInstance getItems(@RestPath Long id) {
        onlyHxRequest();
        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();
        List<Item> availableItems = Item.list("activeFlag", true);
        return Templates.invoiceDetail(invoice, availableItems);
    }

    /**
     * Fetches UOMs based on the selected Item ID (query param so URL is static)
     */
    @GET
    @Path("/item/uoms")
    public TemplateInstance getUomsForItem(@RestQuery Long itemId) {
        onlyHxRequest();
        if (itemId == null) return Templates.uomOptions(List.of());
        Item item = Item.findById(itemId);
        if (item == null) return Templates.uomOptions(List.of());
        List<ItemUom> uoms = ItemUom.find("item", item).list();
        return Templates.uomOptions(uoms);
    }

    @POST
    @Path("/{id}/items")
    @Transactional
    public TemplateInstance addInvoiceItem(
            @RestPath Long id,
            @RestForm @NotNull Long itemId,
            @RestForm @NotNull Long uomId, // Added UOM ID
            @RestForm @NotNull BigDecimal quantity,
            @RestForm @NotNull BigDecimal unitPrice
    ) {
        onlyHxRequest();

        SalesInvoice salesInvoice = SalesInvoice.findById(id);
        var salesInvoiceItem = new SalesInvoiceItem();
        salesInvoiceItem.item = Item.findById(itemId);
        salesInvoiceItem.uom = ItemUom.findById(uomId); // Assign selected UOM
        salesInvoiceItem.quantity = quantity;
        salesInvoiceItem.unitPrice = unitPrice;
        salesInvoiceItem.lineTotal = quantity.multiply(unitPrice);

        salesInvoice.addItem(salesInvoiceItem);
        salesInvoiceItem.invoice = salesInvoice;
        salesInvoice.persist();

        return Templates.invoiceDetail(salesInvoice, Item.list("activeFlag", true));
    }

    private TemplateInstance render(int page, boolean fragmentOnly, List<CustomerBranch> branches, String today) {
        long totalCount = SalesInvoice.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);
        List<SalesInvoice> invoices = SalesInvoice.find("order by invoiceDate desc, id desc")
                .page(currentPage - 1, PAGE_SIZE).list();
        return fragmentOnly
                ? Templates.index$rows(invoices, currentPage, totalPages)
                : Templates.index(invoices, branches, currentPage, totalPages, today);
    }
}
