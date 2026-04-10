package rest;

import com.rosswood.entity.*;
import com.rosswood.service.StockTransactionService;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    @Inject
    StockTransactionService stockService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<SalesInvoice> invoices, List<CustomerBranch> branches, int page, int totalPages, String now, String q, String errorMessage);
        public static native TemplateInstance index$rows(List<SalesInvoice> invoices, int page, int totalPages, String q, String errorMessage);
        public static native TemplateInstance invoiceFormFragment(List<CustomerBranch> branches);
        public static native TemplateInstance invoiceDetail(SalesInvoice invoice, List<Item> availableItems);
        public static native TemplateInstance uomOptions(List<ItemUom> uoms);
    }

    @GET
    @Path("/form-fragment")
    public TemplateInstance getFormFragment() {
        onlyHxRequest();
        return Templates.invoiceFormFragment(CustomerBranch.listAll());
    }

    @GET
    public TemplateInstance index(@RestQuery Integer page, @RestQuery String q) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        List<CustomerBranch> branches = CustomerBranch.listAll();
        String today = LocalDate.now().toString();
        return render(currentPage, isHxRequest(), branches, today, q, null);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm String vatInvoiceNo,
            @RestForm Long branchId,
            @RestForm String invoiceDate,
            @RestForm String remarks,
            @RestForm String paymentMethod,
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
        invoice.status = SalesInvoice.InvoiceStatus.DRAFT;
        try {
            invoice.paymentMethod = SalesInvoice.PaymentMethod.valueOf(paymentMethod);
        } catch (Exception e) {
            invoice.paymentMethod = SalesInvoice.PaymentMethod.CASH;
        }
        invoice.persistAndFlush();

        return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
    }

    @GET
    @Path("/{id}/items")
    public TemplateInstance getItems(@RestPath Long id) {
        onlyHxRequest();
        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();
        return Templates.invoiceDetail(invoice, Item.list("activeFlag", true));
    }

    @GET
    @Path("/item/uoms")
    public TemplateInstance getUomsForItem(@RestQuery Long itemId) {
        onlyHxRequest();
        if (itemId == null) return Templates.uomOptions(List.of());
        Item item = Item.findById(itemId);
        if (item == null) return Templates.uomOptions(List.of());
        return Templates.uomOptions(ItemUom.find("item", item).list());
    }

    @POST
    @Path("/{id}/items")
    @Transactional
    public TemplateInstance addInvoiceItem(
            @RestPath Long id,
            @RestForm @NotNull Long itemId,
            @RestForm @NotNull Long uomId,
            @RestForm @NotNull BigDecimal quantity,
            @RestForm @NotNull BigDecimal unitPriceExVat,
            @RestForm BigDecimal vatRate,
            @RestForm BigDecimal discountPct
    ) {
        onlyHxRequest();

        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();

        if (!invoice.isEditable()) {
            // Silently return — locked invoice, no changes
            return Templates.invoiceDetail(invoice, Item.list("activeFlag", true));
        }

        SalesInvoiceItem line = new SalesInvoiceItem();
        line.item           = Item.findById(itemId);
        line.uom            = ItemUom.findById(uomId);
        line.quantity       = quantity;
        line.unitPriceExVat = unitPriceExVat;
        line.vatRate        = vatRate != null ? vatRate : BigDecimal.ZERO;
        line.discountPct    = (discountPct != null && discountPct.compareTo(BigDecimal.ZERO) > 0) ? discountPct : null;
        line.calculate();   // computes lineTotal, vatAmount, lineTotalIncVat

        invoice.addItem(line); // adds line and calls recalculateTotals()
        invoice.persist();

        return Templates.invoiceDetail(invoice, Item.list("activeFlag", true));
    }

    /**
     * Confirm invoice — posts stock OUT transactions for each line item.
     * Once confirmed the invoice is locked for editing.
     * Credit invoices are validated against the customer's credit limit before confirmation.
     */
    @POST
    @Path("/{id}/confirm")
    @Transactional
    public TemplateInstance confirm(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();

        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();

        if (!invoice.isEditable()) {
            return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
        }

        // Credit limit check — only applies when payment method is CREDIT
        if (invoice.paymentMethod == SalesInvoice.PaymentMethod.CREDIT) {
            com.rosswood.entity.Customer customer = invoice.customerBranch.customer;

            if (customer.customerType != com.rosswood.entity.Customer.CustomerType.CREDIT) {
                String error = customer.name + " is a CASH customer and cannot be invoiced on credit.";
                return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, error);
            }

            if (customer.creditLimit != null) {
                java.math.BigDecimal outstanding = SalesInvoice.outstandingCreditBalance(customer.id);
                java.math.BigDecimal newTotal    = outstanding.add(invoice.totalAmount);
                if (newTotal.compareTo(customer.creditLimit) > 0) {
                    String error = String.format(
                            "Credit limit exceeded for %s. Limit: GHS %s | Outstanding: GHS %s | This invoice: GHS %s",
                            customer.name, customer.creditLimit, outstanding, invoice.totalAmount);
                    return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, error);
                }
            }
        }

        // Post stock OUT for each line
        invoice.items.forEach(line -> stockService.postSale(invoice, line));

        invoice.status = SalesInvoice.InvoiceStatus.CONFIRMED;
        invoice.persist();

        return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
    }

    /**
     * Update payment method and capture MoMo/Cheque details on a DRAFT invoice.
     */
    @POST
    @Path("/{id}/payment")
    @Transactional
    public TemplateInstance updatePayment(
            @RestPath Long id,
            @RestForm String paymentMethod,
            @RestForm String paymentRef,
            @RestForm String paymentInfo,
            @RestForm String paymentDate,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();
        if (!invoice.isEditable()) {
            return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
        }

        SalesInvoice.PaymentMethod pm;
        try { pm = SalesInvoice.PaymentMethod.valueOf(paymentMethod); }
        catch (Exception e) { pm = SalesInvoice.PaymentMethod.CASH; }

        if (pm == SalesInvoice.PaymentMethod.CREDIT) {
            com.rosswood.entity.Customer customer = invoice.customerBranch.customer;
            if (customer.customerType != com.rosswood.entity.Customer.CustomerType.CREDIT) {
                String error = customer.name + " is a CASH customer and cannot be invoiced on credit.";
                return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, error);
            }
        }

        invoice.paymentMethod = pm;
        invoice.paymentRef  = (paymentRef  != null && !paymentRef.isBlank())  ? paymentRef.trim()  : null;
        invoice.paymentInfo = (paymentInfo != null && !paymentInfo.isBlank()) ? paymentInfo.trim() : null;
        invoice.paymentDate = (paymentDate != null && !paymentDate.isBlank()) ? LocalDate.parse(paymentDate) : null;
        invoice.persist();

        return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
    }

    /**
     * Cancel invoice — reverses stock transactions if already confirmed.
     */
    @POST
    @Path("/{id}/cancel")
    @Transactional
    public TemplateInstance cancel(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();

        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();

        if (invoice.status == SalesInvoice.InvoiceStatus.CONFIRMED) {
            // Reverse stock transactions
            invoice.items.forEach(line -> stockService.reverseSale(invoice, line));
        }

        invoice.status = SalesInvoice.InvoiceStatus.CANCELLED;
        invoice.persist();

        return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
    }


    /**
     * Mark invoice as delivered — records delivery date and note number.
     */
    @POST
    @Path("/{id}/deliver")
    @Transactional
    public TemplateInstance deliver(
            @RestPath Long id,
            @RestForm String deliveryDate,
            @RestForm String deliveryNoteNo,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        SalesInvoice invoice = SalesInvoice.findById(id);
        if (invoice == null) throw new NotFoundException();

        if (invoice.status == SalesInvoice.InvoiceStatus.CONFIRMED) {
            invoice.deliveryDate   = LocalDate.parse(deliveryDate);
            invoice.deliveryNoteNo = deliveryNoteNo;
            invoice.status         = SalesInvoice.InvoiceStatus.DELIVERED;
            invoice.persist();
        }

        return render(Optional.ofNullable(page).orElse(1), true, List.of(), LocalDate.now().toString(), null, null);
    }


    private TemplateInstance render(int page, boolean fragmentOnly, List<CustomerBranch> branches, String today, String q, String errorMessage) {
        String term = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase() + "%" : null;
        long totalCount = term != null
                ? SalesInvoice.count("lower(invoiceNo) like ?1 or lower(vatInvoiceNo) like ?1 or lower(customerBranch.branchName) like ?1", term)
                : SalesInvoice.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);
        List<SalesInvoice> invoices = term != null
                ? SalesInvoice.find("(lower(invoiceNo) like ?1 or lower(vatInvoiceNo) like ?1 or lower(customerBranch.branchName) like ?1) order by invoiceDate desc, id desc", term)
                        .page(currentPage - 1, PAGE_SIZE).list()
                : SalesInvoice.find("order by invoiceDate desc, id desc").page(currentPage - 1, PAGE_SIZE).list();
        return fragmentOnly
                ? Templates.index$rows(invoices, currentPage, totalPages, q, errorMessage)
                : Templates.index(invoices, branches, currentPage, totalPages, today, q, errorMessage);
    }
}
