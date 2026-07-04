package rest;

import com.rosswood.entity.*;
import com.rosswood.service.StockTransactionService;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("/api/mobile")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MobileApiController {

    // In-memory token store — cleared on server restart (fine for dev/LAN use)
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Inject
    StockTransactionService stockService;

    @Context
    HttpHeaders headers;

    private String tokenUser() {
        String auth = headers.getHeaderString("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return TOKENS.get(auth.substring(7));
    }

    private boolean isAnon() {
        return tokenUser() == null;
    }

    private Response unauthorized() {
        return Response.status(401)
                .entity(Map.of("error", "Not authenticated"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest req) {
        User user = User.findByUserName(req.username);
        if (user == null || !BcryptUtil.matches(req.password, user.password)) {
           return Response.status(401)
                   .entity(Map.of("error", "Invalid username or password"))
                   .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        String token = UUID.randomUUID().toString();
        TOKENS.put(token, user.userName);
        return Response.ok(Map.of(
                "token", token,
                "username", user.userName,
                "name", user.fullName()
        )).build();
    }

    // ── Logout ────────────────────────────────────────────────────────────
    @POST
    @Path("/logout")
    public Response logout() {
        String auth = headers.getHeaderString("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            TOKENS.remove(auth.substring(7));
        }
        return Response.ok().build();
    }

    // ── Current user info ─────────────────────────────────────────────────
    @GET
    @Path("/me")
    public Response whoAmI() {
        String username = tokenUser();
        if (username == null) return unauthorized();
        User user = User.findByUserName(username);
        return Response.ok(Map.of(
                "username", user.userName,
                "name", user.fullName()
        )).build();
    }

    // ── Seed: customers + items for offline cache ─────────────────────────
    @GET
    @Path("/seed")
    @Transactional
    public Response seed() {
        if (isAnon()) return unauthorized();

        List<CustomerDto> customers = CustomerBranch
                .<CustomerBranch>list("activeFlag = true order by customer.name, branchName")
                .stream()
                .map(b -> new CustomerDto(
                        b.customer.id, b.customer.customerCode, b.customer.name,
                        b.customer.customerType.name(),
                        b.customer.creditLimit,
                        b.id, b.branchName, b.branchAddress,
                        b.contactPerson, b.contactPhone
                ))
                .toList();

        List<ItemDto> items = Item
                .<Item>list("activeFlag = true and itemType.itemTypeCode = 'FINISHED_GOOD' order by itemName")
                .stream()
                .map(i -> {
                    List<UomDto> uoms = ItemUom
                            .<ItemUom>list("item.id = ?1", i.id)
                            .stream()
                            .map(u -> new UomDto(u.id, u.uomCode))
                            .toList();
                    return new ItemDto(
                            i.id, i.itemCode, i.itemName,
                            i.sellingPriceExVat, i.vatRate, uoms
                    );
                })
                .toList();

        return Response.ok(new SeedData(customers, items)).build();
    }

    // ── My invoices ───────────────────────────────────────────────────────
    @GET
    @Path("/invoices")
    @Transactional
    public Response myInvoices() {
        String username = tokenUser();
        if (username == null) return unauthorized();

        User user = User.findByUserName(username);
        boolean salesOnly = user != null
                && user.roles().contains("sales")
                && !user.roles().contains("inventory")
                && !user.roles().contains("production")
                && !user.roles().contains("finance")
                && !user.roles().contains("admin");

        List<SalesInvoice> invoices = salesOnly
                ? SalesInvoice.find("LOWER(createdBy) = ?1 order by invoiceDate desc, id desc", username.toLowerCase()).list()
                : SalesInvoice.find("order by invoiceDate desc, id desc").list();

        List<InvoiceDto> list = invoices.stream()
                .map(inv -> new InvoiceDto(
                        inv.id, inv.invoiceNo,
                        inv.invoiceDate.toString(),
                        inv.customerBranch.customer.name,
                        inv.customerBranch.branchName,
                        inv.status.name(),
                        inv.paymentMethod.name(),
                        inv.customerBranch.customer.customerType.name(),
                        inv.totalAmount,
                        inv.totalAmountExVat,
                        inv.vatAmount,
                        inv.items.stream().map(li -> new LineDto(
                                li.item.itemName, li.uom.uomCode,
                                li.quantity, li.unitPriceExVat,
                                li.vatRate, li.discountPct,
                                li.lineTotal, li.lineTotalIncVat,
                                li.batchCode
                        )).toList(),
                        inv.paymentRef,
                        inv.paymentInfo,
                        inv.paymentDate != null ? inv.paymentDate.toString() : null
                ))
                .toList();
        return Response.ok(list).build();
    }

    // ── Sync offline invoices ─────────────────────────────────────────────
    @POST
    @Path("/sync")
    @Transactional
    public Response sync(List<OfflineInvoice> payload) {
        String username = tokenUser();
        if (username == null) return unauthorized();

        List<SyncResult> results = new ArrayList<>();

        for (int i = 0; i < payload.size(); i++) {
            OfflineInvoice o = payload.get(i);
            try {
                CustomerBranch branch = CustomerBranch.findById(o.customerBranchId());
                if (branch == null) {
                    results.add(new SyncResult(o.localId(), null, "Branch not found"));
                    continue;
                }

                SalesInvoice inv = new SalesInvoice();
                // Synced records land as DRAFTs — the official RW-INV number is
                // only issued when the invoice is confirmed.
                inv.invoiceNo       = "TMP-" + java.util.UUID.randomUUID();
                inv.customerBranch  = branch;
                inv.invoiceDate     = LocalDate.parse(o.invoiceDate());
                inv.paymentMethod   = SalesInvoice.PaymentMethod.valueOf(o.paymentMethod());
                inv.remarks         = o.remarks();
                inv.status          = SalesInvoice.InvoiceStatus.DRAFT;
                inv.totalAmountExVat = BigDecimal.ZERO;
                inv.vatAmount        = BigDecimal.ZERO;
                inv.totalAmount      = BigDecimal.ZERO;
                inv.createdBy        = username;
                inv.persistAndFlush();
                inv.invoiceNo       = SalesInvoice.draftReference(inv.id);

                for (OfflineInvoice.LineItem li : o.items()) {
                    Item item   = Item.findById(li.itemId());
                    ItemUom uom = ItemUom.findById(li.uomId());
                    if (item == null || uom == null) continue;

                    SalesInvoiceItem line = new SalesInvoiceItem();
                    line.item            = item;
                    line.uom             = uom;
                    line.quantity        = li.quantity();
                    line.unitPriceExVat  = li.unitPriceExVat();
                    line.vatRate         = li.vatRate() != null ? li.vatRate() : BigDecimal.ZERO;
                    line.discountPct     = li.discountPct();
                    line.batchCode       = li.batchCode();
                    line.calculate();
                    inv.addItem(line);
                }
                inv.persist();
                results.add(new SyncResult(o.localId(), inv.invoiceNo, null));

            } catch (Exception e) {
                results.add(new SyncResult(o.localId(), null, e.getMessage()));
            }
        }
        return Response.ok(results).build();
    }

    // ── Update payment method ─────────────────────────────────────────────
    @PATCH
    @Path("/invoices/{id}/payment-method")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updatePaymentMethod(@PathParam("id") Long id, PaymentMethodRequest req) {
        if (tokenUser() == null) return unauthorized();
        SalesInvoice inv = SalesInvoice.findById(id);
        if (inv == null) return Response.status(404).entity(Map.of("error","Not found")).type(MediaType.APPLICATION_JSON).build();
        if (inv.status != SalesInvoice.InvoiceStatus.DRAFT)
            return Response.status(400).entity(Map.of("error","Can only change payment method on DRAFT invoices")).type(MediaType.APPLICATION_JSON).build();
        try {
            inv.paymentMethod = SalesInvoice.PaymentMethod.valueOf(req.paymentMethod);
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(Map.of("error","Invalid payment method")).type(MediaType.APPLICATION_JSON).build();
        }
        inv.paymentRef  = (req.paymentRef  != null && !req.paymentRef.isBlank())  ? req.paymentRef.trim()  : null;
        inv.paymentInfo = (req.paymentInfo != null && !req.paymentInfo.isBlank()) ? req.paymentInfo.trim() : null;
        try {
            inv.paymentDate = (req.paymentDate != null && !req.paymentDate.isBlank())
                    ? LocalDate.parse(req.paymentDate) : null;
        } catch (Exception e) {
            inv.paymentDate = null;
        }
        inv.persist();
        return Response.ok(Map.of("paymentMethod", inv.paymentMethod.name())).build();
    }

    // ── Confirm invoice ──────────────────────────────────────────────────
    @POST
    @Path("/invoices/{id}/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response confirmInvoice(@PathParam("id") Long id, ConfirmRequest req) {
        if (tokenUser() == null) return unauthorized();
        SalesInvoice inv = SalesInvoice.findById(id);
        if (inv == null) return Response.status(404).entity(Map.of("error","Not found")).type(MediaType.APPLICATION_JSON).build();
        if (inv.status != SalesInvoice.InvoiceStatus.DRAFT)
            return Response.status(400).entity(Map.of("error","Invoice is not DRAFT")).type(MediaType.APPLICATION_JSON).build();
        // Enforce: CREDIT payment only allowed for CREDIT customers
        if (inv.paymentMethod == SalesInvoice.PaymentMethod.CREDIT
                && inv.customerBranch.customer.customerType != com.rosswood.entity.Customer.CustomerType.CREDIT) {
            return Response.status(400)
                    .entity(Map.of("error", inv.customerBranch.customer.name + " is a CASH customer and cannot be invoiced on credit."))
                    .type(MediaType.APPLICATION_JSON).build();
        }
        // Post stock OUT for each line (mirrors web confirm behaviour)
        inv.items.forEach(line -> stockService.postSale(inv, line));
        // Capture the official GRA VAT invoice number entered at confirm time.
        if (req != null && req.vatInvoiceNo != null && !req.vatInvoiceNo.isBlank()) {
            inv.vatInvoiceNo = req.vatInvoiceNo.trim();
        }
        // Issue the official invoice number now that the sale is real.
        if (!inv.hasOfficialNumber()) {
            inv.invoiceNo = SalesInvoice.nextOfficialInvoiceNo();
        }
        inv.status = SalesInvoice.InvoiceStatus.CONFIRMED;
        inv.persist();
        return Response.ok(Map.of("status", inv.status.name(), "invoiceNo", inv.invoiceNo)).build();
    }

    // ── Mark delivered / payment received ────────────────────────────────
    @POST
    @Path("/invoices/{id}/deliver")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response deliverInvoice(@PathParam("id") Long id, DeliverRequest req) {
        if (tokenUser() == null) return unauthorized();
        SalesInvoice inv = SalesInvoice.findById(id);
        if (inv == null) return Response.status(404).entity(Map.of("error","Not found")).type(MediaType.APPLICATION_JSON).build();
        if (inv.status != SalesInvoice.InvoiceStatus.CONFIRMED)
            return Response.status(400).entity(Map.of("error","Invoice is not CONFIRMED")).type(MediaType.APPLICATION_JSON).build();
        inv.status = SalesInvoice.InvoiceStatus.DELIVERED;
        if (req != null && req.deliveryDate != null && !req.deliveryDate.isBlank())
            inv.deliveryDate = LocalDate.parse(req.deliveryDate);
        if (req != null && req.deliveryNoteNo != null && !req.deliveryNoteNo.isBlank())
            inv.deliveryNoteNo = req.deliveryNoteNo;
        inv.persist();
        return Response.ok(Map.of("status", inv.status.name())).build();
    }

    // ── Add item to DRAFT invoice ─────────────────────────────────────────
    @POST
    @Path("/invoices/{id}/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response addLineItem(@PathParam("id") Long id, AddItemRequest req) {
        if (tokenUser() == null) return unauthorized();
        SalesInvoice inv = SalesInvoice.findById(id);
        if (inv == null) return Response.status(404).entity(Map.of("error","Not found")).type(MediaType.APPLICATION_JSON).build();
        if (inv.status != SalesInvoice.InvoiceStatus.DRAFT)
            return Response.status(400).entity(Map.of("error","Can only add items to DRAFT invoices")).type(MediaType.APPLICATION_JSON).build();
        Item item   = Item.findById(req.itemId);
        ItemUom uom = ItemUom.findById(req.uomId);
        if (item == null || uom == null)
            return Response.status(400).entity(Map.of("error","Item or UOM not found")).type(MediaType.APPLICATION_JSON).build();
        SalesInvoiceItem line = new SalesInvoiceItem();
        line.item           = item;
        line.uom            = uom;
        line.quantity       = req.quantity;
        line.unitPriceExVat = req.unitPriceExVat;
        line.vatRate        = req.vatRate != null ? req.vatRate : BigDecimal.ZERO;
        line.discountPct    = req.discountPct != null ? req.discountPct : BigDecimal.ZERO;
        line.batchCode      = req.batchCode;
        line.calculate();
        inv.addItem(line);
        inv.persist();
        return Response.ok(Map.of(
                "totalAmount",      inv.totalAmount,
                "totalAmountExVat", inv.totalAmountExVat,
                "vatAmount",        inv.vatAmount
        )).build();
    }

    // ── My route for today ────────────────────────────────────────────────
    @GET
    @Path("/route/today")
    @Transactional
    public Response routeToday() {
        String username = tokenUser();
        if (username == null) return unauthorized();
        User user = User.findByUserName(username);
        if (user == null) return unauthorized();

        LocalDate date  = LocalDate.now();
        DayOfWeek today = date.getDayOfWeek();

        List<RouteDto> routes = JourneyPlan.findByRepAndDay(user.id, today).stream()
                .map(plan -> {
                    List<JourneyStop> stops =
                            JourneyStop.list("journeyPlan.id = ?1 order by visitOrder", plan.id);
                    List<RouteStopDto> stopDtos = stops.stream().map(s -> {
                        JourneyVisit v = JourneyVisit.forStopOnDate(s.id, date);
                        return new RouteStopDto(
                                s.id, s.visitOrder,
                                s.customerBranch.id,
                                s.customerBranch.customer.name,
                                s.customerBranch.branchName,
                                s.customerBranch.branchAddress,
                                s.customerBranch.contactPerson,
                                s.customerBranch.contactPhone,
                                v != null ? v.status.name() : null,
                                v != null ? v.remarks : null
                        );
                    }).toList();
                    return new RouteDto(plan.id, plan.name, plan.weekday.name(), stopDtos);
                })
                .toList();

        return Response.ok(new RouteTodayResponse(date.toString(), today.name(), routes)).build();
    }

    // ── Check a stop off (visited / skipped) or clear it ──────────────────
    @POST
    @Path("/route/visit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response markVisit(VisitRequest req) {
        String username = tokenUser();
        if (username == null) return unauthorized();
        if (req == null || req.stopId == null)
            return Response.status(400).entity(Map.of("error", "stopId is required")).type(MediaType.APPLICATION_JSON).build();

        JourneyStop stop = JourneyStop.findById(req.stopId);
        if (stop == null)
            return Response.status(404).entity(Map.of("error", "Stop not found")).type(MediaType.APPLICATION_JSON).build();

        LocalDate date  = LocalDate.now();
        JourneyVisit visit = JourneyVisit.forStopOnDate(req.stopId, date);

        // A blank / "PENDING" status clears the visit for today.
        if (req.status == null || req.status.isBlank() || "PENDING".equalsIgnoreCase(req.status)) {
            if (visit != null) visit.delete();
            return Response.ok(Map.of("stopId", req.stopId, "status", "PENDING")).build();
        }

        JourneyVisit.VisitStatus status;
        try {
            status = JourneyVisit.VisitStatus.valueOf(req.status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(Map.of("error", "Invalid status")).type(MediaType.APPLICATION_JSON).build();
        }

        if (visit == null) {
            visit = new JourneyVisit();
            visit.journeyStop = stop;
            visit.visitDate = date;
        }
        visit.status  = status;
        visit.remarks = (req.remarks != null && !req.remarks.isBlank()) ? req.remarks.trim() : null;
        visit.persist();

        return Response.ok(Map.of("stopId", req.stopId, "status", visit.status.name())).build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public record SeedData(List<CustomerDto> customers, List<ItemDto> items) {}

    public record CustomerDto(
            Long customerId, String customerCode, String name,
            String customerType, BigDecimal creditLimit,
            Long branchId, String branchName, String branchAddress,
            String contactPerson, String contactPhone) {}

    public record ItemDto(
            Long id, String itemCode, String itemName,
            BigDecimal sellingPriceExVat, BigDecimal vatRate,
            List<UomDto> uoms) {}

    public record UomDto(Long id, String uomCode) {}

    public record InvoiceDto(
            Long id, String invoiceNo, String invoiceDate,
            String customerName, String branchName,
            String status, String paymentMethod,
            String customerType,
            BigDecimal totalAmount, BigDecimal totalAmountExVat, BigDecimal vatAmount,
            List<LineDto> items,
            String paymentRef, String paymentInfo, String paymentDate) {}

    public record LineDto(
            String itemName, String uomCode,
            BigDecimal quantity, BigDecimal unitPriceExVat,
            BigDecimal vatRate, BigDecimal discountPct,
            BigDecimal lineTotal, BigDecimal lineTotalIncVat,
            String batchCode) {}

    public record OfflineInvoice(
            String localId, Long customerBranchId, String invoiceDate,
            String paymentMethod, String remarks,
            List<LineItem> items
    ) {
        public record LineItem(
                Long itemId, Long uomId,
                BigDecimal quantity, BigDecimal unitPriceExVat,
                BigDecimal vatRate, BigDecimal discountPct,
                String batchCode) {}
    }

    public record SyncResult(String localId, String invoiceNo, String error) {}

    public record RouteTodayResponse(String date, String weekday, List<RouteDto> routes) {}

    public record RouteDto(Long planId, String name, String weekday, List<RouteStopDto> stops) {}

    public record RouteStopDto(
            Long stopId, Integer visitOrder,
            Long branchId, String customerName, String branchName,
            String branchAddress, String contactPerson, String contactPhone,
            String visitStatus, String remarks) {}

    public static class VisitRequest {
        public Long stopId;
        public String status;   // VISITED, SKIPPED, or blank/PENDING to clear
        public String remarks;  // optional note
    }

    public static class ConfirmRequest {
        public String vatInvoiceNo;    // official GRA VAT invoice number, optional
    }

    public static class DeliverRequest {
        public String deliveryDate;    // YYYY-MM-DD, optional
        public String deliveryNoteNo;  // waybill reference, optional
    }

    public static class PaymentMethodRequest {
        public String paymentMethod;
        public String paymentRef;   // MOMO: transaction ref / CHEQUE: cheque no
        public String paymentInfo;  // MOMO: sender phone   / CHEQUE: bank name
        public String paymentDate;  // CHEQUE: cheque date (YYYY-MM-DD)
    }

    public static class AddItemRequest {
        public Long itemId;
        public Long uomId;
        public BigDecimal quantity;
        public BigDecimal unitPriceExVat;
        public BigDecimal vatRate;
        public BigDecimal discountPct;
        public String batchCode;
    }
}
