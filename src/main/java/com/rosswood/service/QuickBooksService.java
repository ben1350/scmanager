package com.rosswood.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosswood.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Direct QuickBooks Online integration service.
 *
 * <p>Uses the standard Java {@code java.net.http.HttpClient} (Java 11+) and Jackson
 * for JSON serialisation.  OAuth 2.0 Authorization Code flow is implemented
 * manually so there is no additional OAuth library dependency.</p>
 *
 * <p>Config keys (application.properties):
 * <pre>
 *   quickbooks.client-id     = YOUR_CLIENT_ID
 *   quickbooks.client-secret = YOUR_CLIENT_SECRET
 *   quickbooks.redirect-uri  = https://yourdomain.com/quickbooks/callback
 *   quickbooks.sandbox       = true          # false for production
 * </pre>
 */
@ApplicationScoped
public class QuickBooksService {

    // ── Config ────────────────────────────────────────────────────────────

    @ConfigProperty(name = "quickbooks.client-id", defaultValue = "")
    String clientId;

    @ConfigProperty(name = "quickbooks.client-secret", defaultValue = "")
    String clientSecret;

    @ConfigProperty(name = "quickbooks.redirect-uri", defaultValue = "http://localhost:8080/quickbooks/callback")
    String redirectUri;

    @ConfigProperty(name = "quickbooks.sandbox", defaultValue = "true")
    boolean sandboxDefault;

    private static final String QBO_AUTH_BASE   = "https://appcenter.intuit.com/connect/oauth2";
    private static final String QBO_TOKEN_URL   = "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";
    private static final String QBO_REVOKE_URL  = "https://developer.api.intuit.com/v2/oauth2/tokens/revoke";
    private static final String QBO_SCOPES      = "com.intuit.quickbooks.accounting";
    private static final String API_LIVE        = "https://quickbooks.api.intuit.com/v3/company/";
    private static final String API_SANDBOX     = "https://sandbox-quickbooks.api.intuit.com/v3/company/";

    // ── Internal helpers ──────────────────────────────────────────────────

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    private String apiBase(QuickBooksConfig cfg) {
        return (cfg.sandbox ? API_SANDBOX : API_LIVE) + cfg.realmId + "/";
    }

    private String basicAuth() {
        String creds = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    // ── OAuth flow ────────────────────────────────────────────────────────

    /** Builds the Intuit authorization URL to redirect the browser to. */
    public String buildAuthUrl(String state) {
        return QBO_AUTH_BASE
                + "?client_id="     + encode(clientId)
                + "&redirect_uri="  + encode(redirectUri)
                + "&response_type=code"
                + "&scope="         + encode(QBO_SCOPES)
                + "&state="         + encode(state);
    }

    /**
     * Exchanges the authorization code received in the OAuth callback for
     * access + refresh tokens, and persists them as the singleton config row.
     */
    @Transactional
    public QuickBooksConfig exchangeCode(String code, String realmId, String username) throws Exception {
        String body = "grant_type=authorization_code"
                + "&code="         + encode(code)
                + "&redirect_uri=" + encode(redirectUri);

        JsonNode resp = postForm(QBO_TOKEN_URL, body, true);

        QuickBooksConfig cfg = Optional.ofNullable(QuickBooksConfig.<QuickBooksConfig>find("id = 1").firstResult())
                .orElseGet(QuickBooksConfig::new);

        cfg.realmId      = realmId;
        cfg.accessToken  = resp.get("access_token").asText();
        cfg.refreshToken = resp.get("refresh_token").asText();
        cfg.tokenExpiry  = LocalDateTime.now().plusSeconds(resp.get("expires_in").asInt(3600));
        cfg.sandbox      = sandboxDefault;
        cfg.connectedAt  = LocalDateTime.now();
        cfg.connectedBy  = username;

        if (cfg.id == null) cfg.persistAndFlush();
        else cfg.persist();

        return cfg;
    }

    /**
     * Uses the stored refresh token to obtain a fresh access token.
     * Called automatically by {@link #getValidToken()} when needed.
     */
    @Transactional
    public void refreshToken(QuickBooksConfig cfg) throws Exception {
        String body = "grant_type=refresh_token&refresh_token=" + encode(cfg.refreshToken);
        JsonNode resp = postForm(QBO_TOKEN_URL, body, true);

        cfg.accessToken  = resp.get("access_token").asText();
        cfg.tokenExpiry  = LocalDateTime.now().plusSeconds(resp.get("expires_in").asInt(3600));
        // QBO sometimes returns a new refresh token on refresh — update if present
        if (resp.has("refresh_token")) {
            cfg.refreshToken = resp.get("refresh_token").asText();
        }
        cfg.persist();
    }

    /**
     * Returns a valid access token, refreshing it first if expired.
     *
     * @throws IllegalStateException if no connection config is stored
     */
    public String getValidToken() throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        if (cfg == null || !cfg.isConnected()) {
            throw new IllegalStateException("QuickBooks is not connected.");
        }
        if (!cfg.isAccessTokenValid()) {
            refreshToken(cfg);
            cfg = QuickBooksConfig.getInstance();
        }
        return cfg.accessToken;
    }

    /** Revokes both tokens and clears the stored config. */
    @Transactional
    public void disconnect(String username) throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        if (cfg == null) return;

        // Best-effort revoke
        try {
            String body = "token=" + encode(cfg.refreshToken);
            postForm(QBO_REVOKE_URL, body, true);
        } catch (Exception ignored) {}

        cfg.realmId       = null;
        cfg.accessToken   = null;
        cfg.refreshToken  = null;
        cfg.tokenExpiry   = null;
        cfg.connectedAt   = null;
        cfg.connectedBy   = null;
        cfg.persist();
    }

    // ── Sync: Customers ───────────────────────────────────────────────────

    /** Syncs all active customers to QBO.  Returns the number of customers upserted. */
    @Transactional
    public int syncAllCustomers() throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        List<Customer> customers = Customer.listAll();
        int count = 0;
        for (Customer c : customers) {
            syncCustomer(c, cfg);
            count++;
        }
        return count;
    }

    @Transactional
    public void syncCustomer(Customer customer, QuickBooksConfig cfg) throws Exception {
        String token  = getValidToken();
        String base   = apiBase(cfg);

        QuickBooksEntityMap existing = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.CUSTOMER, customer.id);

        ObjectNode payload = buildCustomerPayload(customer, existing);
        String endpoint    = base + "customer";

        JsonNode resp = postJson(endpoint, payload.toString(), token);
        JsonNode customerNode = resp.get("Customer");

        upsertMap(QuickBooksEntityMap.EntityType.CUSTOMER, customer.id,
                customerNode.get("Id").asText(),
                customerNode.get("SyncToken").asText());
    }

    private ObjectNode buildCustomerPayload(Customer customer, QuickBooksEntityMap existing) {
        ObjectNode node = json.createObjectNode();

        // When updating we need Id + SyncToken
        if (existing != null) {
            node.put("Id", existing.qboId);
            node.put("SyncToken", existing.qboSyncToken != null ? existing.qboSyncToken : "0");
        }

        // QBO DisplayName must be unique — use customerCode as suffix for safety
        node.put("DisplayName", customer.name + " [" + customer.customerCode + "]");
        node.put("CompanyName", customer.name);

        if (customer.phone != null && !customer.phone.isBlank()) {
            node.putObject("PrimaryPhone").put("FreeFormNumber", customer.phone);
        }
        if (customer.email != null && !customer.email.isBlank()) {
            node.putObject("PrimaryEmailAddr").put("Address", customer.email);
        }
        if (customer.address != null && !customer.address.isBlank()) {
            ObjectNode addr = node.putObject("BillAddr");
            addr.put("Line1", customer.address);
        }
        return node;
    }

    // ── Sync: Invoices ────────────────────────────────────────────────────

    /**
     * Syncs all CONFIRMED and DELIVERED invoices to QBO.
     * Skips invoices whose customer has not been synced yet.
     */
    @Transactional
    public int syncAllInvoices() throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        List<SalesInvoice> invoices = SalesInvoice.list(
                "status = ?1 or status = ?2",
                SalesInvoice.InvoiceStatus.CONFIRMED,
                SalesInvoice.InvoiceStatus.DELIVERED);
        int count = 0;
        for (SalesInvoice inv : invoices) {
            try {
                syncInvoice(inv, cfg);
                count++;
            } catch (CustomerNotSyncedException ignored) {
                // Customer hasn't been pushed to QBO yet — skip silently
            }
        }
        return count;
    }

    @Transactional
    public void syncInvoice(SalesInvoice invoice, QuickBooksConfig cfg) throws Exception {
        // Resolve the customer's QBO id
        Customer customer = invoice.customerBranch.customer;
        QuickBooksEntityMap customerMap = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.CUSTOMER, customer.id);
        if (customerMap == null) {
            throw new CustomerNotSyncedException("Customer " + customer.name + " not yet synced to QBO");
        }

        String token    = getValidToken();
        String base     = apiBase(cfg);

        QuickBooksEntityMap existing = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.INVOICE, invoice.id);

        ObjectNode payload = buildInvoicePayload(invoice, customerMap.qboId, existing);
        String endpoint    = base + "invoice";

        JsonNode resp        = postJson(endpoint, payload.toString(), token);
        JsonNode invoiceNode = resp.get("Invoice");

        upsertMap(QuickBooksEntityMap.EntityType.INVOICE, invoice.id,
                invoiceNode.get("Id").asText(),
                invoiceNode.get("SyncToken").asText());
    }

    private ObjectNode buildInvoicePayload(SalesInvoice invoice, String qboCustomerId,
                                           QuickBooksEntityMap existing) {
        ObjectNode node = json.createObjectNode();

        if (existing != null) {
            node.put("Id", existing.qboId);
            node.put("SyncToken", existing.qboSyncToken != null ? existing.qboSyncToken : "0");
        }

        node.putObject("CustomerRef").put("value", qboCustomerId);
        node.put("DocNumber", invoice.invoiceNo);
        node.put("TxnDate", invoice.invoiceDate.toString());

        if (invoice.vatInvoiceNo != null && !invoice.vatInvoiceNo.isBlank()) {
            node.put("PrivateNote", "VAT Invoice: " + invoice.vatInvoiceNo);
        }

        // Build line items
        ArrayNode lines = node.putArray("Line");
        if (invoice.items != null && !invoice.items.isEmpty()) {
            for (SalesInvoiceItem item : invoice.items) {
                ObjectNode line = lines.addObject();
                BigDecimal amount = item.lineTotalIncVat != null ? item.lineTotalIncVat : BigDecimal.ZERO;
                line.put("Amount", amount);
                line.put("DetailType", "SalesItemLineDetail");
                ObjectNode detail = line.putObject("SalesItemLineDetail");
                // Use QBO's built-in "Services" item (Id=1) as a generic line item
                detail.putObject("ItemRef").put("value", "1").put("name", "Services");
                detail.put("Qty", item.quantity != null ? item.quantity : BigDecimal.ONE);
                detail.put("UnitPrice", item.unitPriceExVat != null ? item.unitPriceExVat : BigDecimal.ZERO);
                // Description from item name
                line.put("Description", item.item != null ? item.item.itemName : "");
            }
        } else {
            // QBO requires at least one line — use invoice total if no items loaded
            ObjectNode line = lines.addObject();
            line.put("Amount", invoice.totalAmount);
            line.put("DetailType", "SalesItemLineDetail");
            line.putObject("SalesItemLineDetail")
                .putObject("ItemRef").put("value", "1");
        }

        return node;
    }

    // ── Sync: Payments ────────────────────────────────────────────────────

    /**
     * Syncs payments for all non-CREDIT DELIVERED invoices that have a matching
     * QBO invoice map but no QBO payment map yet.
     */
    @Transactional
    public int syncAllPayments() throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        List<SalesInvoice> invoices = SalesInvoice.list(
                "status = ?1 and paymentMethod != ?2",
                SalesInvoice.InvoiceStatus.DELIVERED,
                SalesInvoice.PaymentMethod.CREDIT);
        int count = 0;
        for (SalesInvoice inv : invoices) {
            // Only sync payments for invoices already in QBO
            QuickBooksEntityMap invoiceMap = QuickBooksEntityMap.findMapping(
                    QuickBooksEntityMap.EntityType.INVOICE, inv.id);
            if (invoiceMap == null) continue;

            // Skip if already synced
            QuickBooksEntityMap paymentMap = QuickBooksEntityMap.findMapping(
                    QuickBooksEntityMap.EntityType.PAYMENT, inv.id);
            if (paymentMap != null) continue;

            try {
                syncPayment(inv, invoiceMap, cfg);
                count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    @Transactional
    public void syncPayment(SalesInvoice invoice, QuickBooksEntityMap invoiceMap,
                            QuickBooksConfig cfg) throws Exception {
        Customer customer = invoice.customerBranch.customer;
        QuickBooksEntityMap customerMap = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.CUSTOMER, customer.id);
        if (customerMap == null) return;

        String token = getValidToken();
        String base  = apiBase(cfg);

        ObjectNode payload  = buildPaymentPayload(invoice, customerMap.qboId, invoiceMap.qboId);
        JsonNode resp        = postJson(base + "payment", payload.toString(), token);
        JsonNode paymentNode = resp.get("Payment");

        upsertMap(QuickBooksEntityMap.EntityType.PAYMENT, invoice.id,
                paymentNode.get("Id").asText(),
                paymentNode.get("SyncToken").asText());
    }

    private ObjectNode buildPaymentPayload(SalesInvoice invoice, String qboCustomerId,
                                           String qboInvoiceId) {
        ObjectNode node = json.createObjectNode();
        node.putObject("CustomerRef").put("value", qboCustomerId);
        node.put("TotalAmt", invoice.totalAmount);

        LocalDate payDate = invoice.paymentDate != null
                ? invoice.paymentDate
                : (invoice.deliveryDate != null ? invoice.deliveryDate : invoice.invoiceDate);
        node.put("TxnDate", payDate.toString());

        // Link to the invoice
        ObjectNode lineItem = node.putArray("Line").addObject();
        lineItem.put("Amount", invoice.totalAmount);
        lineItem.putArray("LinkedTxn").addObject()
                .put("TxnId", qboInvoiceId)
                .put("TxnType", "Invoice");

        // Payment method memo
        node.put("PrivateNote", "Payment via " + invoice.paymentMethod.name()
                + (invoice.paymentRef != null ? " | Ref: " + invoice.paymentRef : ""));

        return node;
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────

    /** POST application/json with Bearer auth, returns parsed response body. */
    private JsonNode postJson(String url, String body, String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("QBO API error " + resp.statusCode() + ": " + resp.body());
        }
        return json.readTree(resp.body());
    }

    /** POST application/x-www-form-urlencoded, optionally with Basic auth. */
    private JsonNode postForm(String url, String body, boolean useBasicAuth) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (useBasicAuth) builder.header("Authorization", basicAuth());

        HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("QBO token error " + resp.statusCode() + ": " + resp.body());
        }
        return json.readTree(resp.body());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ── Map persistence ───────────────────────────────────────────────────

    @Transactional
    void upsertMap(QuickBooksEntityMap.EntityType type, Long localId,
                   String qboId, String syncToken) {
        QuickBooksEntityMap map = QuickBooksEntityMap.findMapping(type, localId);
        if (map == null) {
            map = new QuickBooksEntityMap();
            map.entityType = type;
            map.localId    = localId;
        }
        map.qboId        = qboId;
        map.qboSyncToken = syncToken;
        map.syncedAt     = LocalDateTime.now();
        if (map.id == null) map.persistAndFlush();
        else map.persist();
    }

    // ── Inner types ───────────────────────────────────────────────────────

    public static class CustomerNotSyncedException extends RuntimeException {
        public CustomerNotSyncedException(String msg) { super(msg); }
    }

    public record SyncCounts(int customers, int invoices, int payments) {}
}
