package rest;

import com.rosswood.entity.QuickBooksConfig;
import com.rosswood.entity.QuickBooksEntityMap;
import com.rosswood.service.QuickBooksService;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import com.rosswood.entity.User;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestQuery;

import java.net.URI;
import java.util.UUID;

@Path("/quickbooks")
@Authenticated
public class QuickBooksController extends HxController {

    @Inject
    QuickBooksService qboService;

    @Inject
    SecurityIdentity identity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(
                QuickBooksConfig config,
                long customersSynced,
                long invoicesSynced,
                long paymentsSynced,
                String flash
        );
    }

    // ── Admin guard ───────────────────────────────────────────────────────

    /** Throws 403 if the logged-in user is not an admin. */
    private void requireAdmin() {
        User u = User.find("userName", identity.getPrincipal().getName()).firstResult();
        if (u == null || !u.isAdmin) throw new ForbiddenException("Admin access required.");
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GET
    public TemplateInstance index(@RestQuery String flash) {
        requireAdmin();
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        long customers = QuickBooksEntityMap.countSynced(QuickBooksEntityMap.EntityType.CUSTOMER);
        long invoices  = QuickBooksEntityMap.countSynced(QuickBooksEntityMap.EntityType.INVOICE);
        long payments  = QuickBooksEntityMap.countSynced(QuickBooksEntityMap.EntityType.PAYMENT);
        return Templates.index(cfg, customers, invoices, payments, flash);
    }

    // ── OAuth Connect ─────────────────────────────────────────────────────

    @GET
    @Path("/connect")
    public Response connect() {
        requireAdmin();
        String state   = UUID.randomUUID().toString();
        String authUrl = qboService.buildAuthUrl(state);
        return Response.seeOther(URI.create(authUrl)).build();
    }

    @GET
    @Path("/callback")
    @Transactional
    public Response callback(@RestQuery String code,
                             @RestQuery String realmId,
                             @RestQuery String state,
                             @RestQuery String error) {
        requireAdmin();
        if (error != null || code == null || realmId == null) {
            String msg = error != null ? error : "Authorization was cancelled or failed.";
            return redirect("index", "error=" + encode(msg));
        }
        try {
            qboService.exchangeCode(code, realmId, identity.getPrincipal().getName());
            return redirect("index", "flash=Connected+to+QuickBooks+successfully.");
        } catch (Exception e) {
            return redirect("index", "flash=Connection+failed:+" + encode(e.getMessage()));
        }
    }

    // ── Disconnect ────────────────────────────────────────────────────────

    @POST
    @Path("/disconnect")
    @Transactional
    public Response disconnect() {
        requireAdmin();
        try {
            qboService.disconnect(identity.getPrincipal().getName());
            return redirect("index", "flash=Disconnected+from+QuickBooks.");
        } catch (Exception e) {
            return redirect("index", "flash=Disconnect+failed:+" + encode(e.getMessage()));
        }
    }

    // ── Sync: Customers ───────────────────────────────────────────────────

    @POST
    @Path("/sync/customers")
    @Transactional
    public Response syncCustomers() {
        requireAdmin();
        try {
            int n = qboService.syncAllCustomers();
            return redirect("index", "flash=Synced+" + n + "+customer(s)+to+QuickBooks.");
        } catch (Exception e) {
            return redirect("index", "flash=Customer+sync+failed:+" + encode(e.getMessage()));
        }
    }

    // ── Sync: Invoices ────────────────────────────────────────────────────

    @POST
    @Path("/sync/invoices")
    @Transactional
    public Response syncInvoices() {
        requireAdmin();
        try {
            int n = qboService.syncAllInvoices();
            return redirect("index", "flash=Synced+" + n + "+invoice(s)+to+QuickBooks.");
        } catch (Exception e) {
            return redirect("index", "flash=Invoice+sync+failed:+" + encode(e.getMessage()));
        }
    }

    // ── Sync: Payments ────────────────────────────────────────────────────

    @POST
    @Path("/sync/payments")
    @Transactional
    public Response syncPayments() {
        requireAdmin();
        try {
            int n = qboService.syncAllPayments();
            return redirect("index", "flash=Synced+" + n + "+payment(s)+to+QuickBooks.");
        } catch (Exception e) {
            return redirect("index", "flash=Payment+sync+failed:+" + encode(e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Response redirect(String method, String query) {
        String path = "/quickbooks/index?" + query;
        return Response.seeOther(URI.create(path)).build();
    }

    private static String encode(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
