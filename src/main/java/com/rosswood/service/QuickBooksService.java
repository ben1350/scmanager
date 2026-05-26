package com.rosswood.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosswood.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

/**
 * Direct QuickBooks Online integration service.
 *
 * <p>Uses the standard Java {@code java.net.http.HttpClient} (Java 11+) and Jackson
 * for JSON serialisation.  OAuth 2.0 Authorization Code flow is implemented
 * manually so there is no additional OAuth library dependency.</p>
 *
 * <p>Transaction rule: @Transactional is ONLY on methods that do pure DB writes.
 * Methods that make HTTP calls must never hold a DB connection open — doing so
 * causes "Connection is closed" errors when the QBO call takes any time.</p>
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

    private static final String QBO_AUTH_BASE = "https://appcenter.intuit.com/connect/oauth2";
    private static final String QBO_TOKEN_URL = "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";
    private static final String QBO_REVOKE_URL = "https://developer.api.intuit.com/v2/oauth2/tokens/revoke";
    private static final String QBO_SCOPES    = "com.intuit.quickbooks.accounting";
    private static final String API_LIVE      = "https://quickbooks.api.intuit.com/v3/company/";
    private static final String API_SANDBOX   = "https://sandbox-quickbooks.api.intuit.com/v3/company/";

    @Inject
    QuickBooksMapRepository mapRepo;

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

    public String buildAuthUrl(String state) {
        return QBO_AUTH_BASE
                + "?client_id="    + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope="        + encode(QBO_SCOPES)
                + "&state="        + encode(state);
    }

    /**
     * Exchanges the authorization code for tokens.
     * HTTP call happens first (outside transaction), then DB write.
     */
    public QuickBooksConfig exchangeCode(String code, String realmId, String username) throws Exception {
        String body = "grant_type=authorization_code"
                + "&code="         + encode(code)
                + "&redirect_uri=" + encode(redirectUri);

        // HTTP call — no DB connection held
        JsonNode resp = postForm(QBO_TOKEN_URL, body, true);

        String accessToken  = resp.get("access_token").asText();
        String refreshToken = resp.get("refresh_token").asText();
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(resp.get("expires_in").asInt(3600));

        // Short DB write — connection only held for the persist
        return saveTokens(realmId, accessToken, refreshToken, expiry, username);
    }

    @Transactional
    public QuickBooksConfig saveTokens(String realmId, String accessToken, String refreshToken,
                                       LocalDateTime expiry, String username) {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        if (cfg == null) cfg = new QuickBooksConfig();
        cfg.realmId      = realmId;
        cfg.accessToken  = accessToken;
        cfg.refreshToken = refreshToken;
        cfg.tokenExpiry  = expiry;
        cfg.sandbox      = sandboxDefault;
        cfg.connectedAt  = LocalDateTime.now();
        cfg.connectedBy  = username;
        if (cfg.id == null) cfg.persistAndFlush();
        else cfg.persist();
        return cfg;
    }

    /**
     * Refreshes the access token. HTTP call first, then short DB write.
     */
    public void refreshToken(QuickBooksConfig cfg) throws Exception {
        String body = "grant_type=refresh_token&refresh_token=" + encode(cfg.refreshToken);

        // HTTP call — outside transaction
        JsonNode resp = postForm(QBO_TOKEN_URL, body, true);

        String newAccess  = resp.get("access_token").asText();
        LocalDateTime exp = LocalDateTime.now().plusSeconds(resp.get("expires_in").asInt(3600));
        String newRefresh = resp.has("refresh_token") ? resp.get("refresh_token").asText() : cfg.refreshToken;

        // Short DB write
        updateTokens(cfg.id, newAccess, newRefresh, exp);
    }

    @Transactional
    public void updateTokens(Long cfgId, String accessToken, String refreshToken, LocalDateTime expiry) {
        QuickBooksConfig cfg = QuickBooksConfig.findById(cfgId);
        if (cfg == null) return;
        cfg.accessToken  = accessToken;
        cfg.refreshToken = refreshToken;
        cfg.tokenExpiry  = expiry;
        cfg.persist();
    }

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

    public void disconnect(String username) throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        if (cfg == null) return;

        // Best-effort HTTP revoke — outside transaction
        try {
            postForm(QBO_REVOKE_URL, "token=" + encode(cfg.refreshToken), true);
        } catch (Exception ignored) {}

        clearTokens(cfg.id);
    }

    @Transactional
    public void clearTokens(Long cfgId) {
        QuickBooksConfig cfg = QuickBooksConfig.findById(cfgId);
        if (cfg == null) return;
        cfg.realmId      = null;
        cfg.accessToken  = null;
        cfg.refreshToken = null;
        cfg.tokenExpiry  = null;
        cfg.connectedAt  = null;
        cfg.connectedBy  = null;
        cfg.persist();
    }

    // ── Sync: Customers ───────────────────────────────────────────────────

    // No @Transactional — loops with HTTP calls; DB connection must not be held open
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

    // No @Transactional — makes HTTP call; upsertMap handles its own transaction
    public void syncCustomer(Customer customer, QuickBooksConfig cfg) throws Exception {
        String token = getValidToken();

        QuickBooksEntityMap existing = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.CUSTOMER, customer.id);

        ObjectNode payload = buildCustomerPayload(customer, existing);

        JsonNode customerNode;
        try {
            // HTTP call — no DB connection held
            JsonNode resp = postJson(apiBase(cfg) + "customer", payload.toString(), token);
            customerNode  = resp.get("Customer");
        } catch (RuntimeException e) {
            // QBO 6240 = Duplicate Name: customer already exists in QBO but we have no local mapping.
            // Query QBO by DisplayName to recover the Id + SyncToken, then retry as an update.
            if (e.getMessage() != null && e.getMessage().contains("6240")) {
                customerNode = findQboCustomerByDisplayName(
                        customer.name + " [" + customer.customerCode + "]", cfg, token);
                if (customerNode == null)
                    throw new RuntimeException("Customer exists in QBO but could not be retrieved: " + customer.name);
                // Retry as update now that we have Id + SyncToken
                existing = null; // force rebuild with QBO id
                ObjectNode update = buildCustomerPayload(customer, null);
                update.put("Id", customerNode.get("Id").asText());
                update.put("SyncToken", customerNode.get("SyncToken").asText());
                JsonNode resp2 = postJson(apiBase(cfg) + "customer", update.toString(), token);
                customerNode   = resp2.get("Customer");
            } else {
                throw e;
            }
        }

        mapRepo.upsertMap(QuickBooksEntityMap.EntityType.CUSTOMER, customer.id,
                customerNode.get("Id").asText(),
                customerNode.get("SyncToken").asText());
    }

    /** Queries QBO for a customer by exact DisplayName. Returns the Customer node or null. */
    private JsonNode findQboCustomerByDisplayName(String displayName, QuickBooksConfig cfg,
                                                   String token) throws Exception {
        String query = "select * from Customer where DisplayName = '"
                + displayName.replace("'", "\\'") + "'";
        String url   = apiBase(cfg) + "query?query=" + encode(query) + "&minorversion=65";

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) return null;

        JsonNode root     = json.readTree(resp.body());
        JsonNode results  = root.path("QueryResponse").path("Customer");
        if (results.isArray() && results.size() > 0) return results.get(0);
        return null;
    }

    private ObjectNode buildCustomerPayload(Customer customer, QuickBooksEntityMap existing) {
        ObjectNode node = json.createObjectNode();
        if (existing != null) {
            node.put("Id", existing.qboId);
            node.put("SyncToken", existing.qboSyncToken != null ? existing.qboSyncToken : "0");
        }
        node.put("DisplayName", customer.name + " [" + customer.customerCode + "]");
        node.put("CompanyName", customer.name);
        if (customer.phone != null && !customer.phone.isBlank())
            node.putObject("PrimaryPhone").put("FreeFormNumber", customer.phone);
        if (customer.email != null && !customer.email.isBlank())
            node.putObject("PrimaryEmailAddr").put("Address", customer.email);
        if (customer.address != null && !customer.address.isBlank())
            node.putObject("BillAddr").put("Line1", customer.address);
        return node;
    }

    // ── Sync: Invoices ────────────────────────────────────────────────────

    // No @Transactional — loops with HTTP calls
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
                // Customer not yet in QBO — skip
            }
        }
        return count;
    }

    // No @Transactional — makes HTTP call
    public void syncInvoice(SalesInvoice invoice, QuickBooksConfig cfg) throws Exception {
        Customer customer = invoice.customerBranch.customer;
        QuickBooksEntityMap customerMap = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.CUSTOMER, customer.id);
        if (customerMap == null)
            throw new CustomerNotSyncedException("Customer " + customer.name + " not yet synced to QBO");

        String token = getValidToken();

        QuickBooksEntityMap existing = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.INVOICE, invoice.id);

        ObjectNode payload = buildInvoicePayload(invoice, customerMap.qboId, existing);

        // HTTP call — no DB connection held
        JsonNode resp        = postJson(apiBase(cfg) + "invoice", payload.toString(), token);
        JsonNode invoiceNode = resp.get("Invoice");

        // Short DB write
        mapRepo.upsertMap(QuickBooksEntityMap.EntityType.INVOICE, invoice.id,
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
        if (invoice.vatInvoiceNo != null && !invoice.vatInvoiceNo.isBlank())
            node.put("PrivateNote", "VAT Invoice: " + invoice.vatInvoiceNo);

        // QBO validates: Amount == UnitPrice * Qty exactly.
        // Use lineTotal (ex-VAT, post-discount) as Amount; derive UnitPrice = Amount / Qty.
        ArrayNode lines = node.putArray("Line");
        if (invoice.items != null && !invoice.items.isEmpty()) {
            for (SalesInvoiceItem item : invoice.items) {
                BigDecimal qty    = item.quantity != null && item.quantity.compareTo(BigDecimal.ZERO) != 0
                        ? item.quantity : BigDecimal.ONE;
                BigDecimal amount = item.lineTotal != null ? item.lineTotal : BigDecimal.ZERO;
                BigDecimal unitPrice = amount.divide(qty, 4, java.math.RoundingMode.HALF_UP);

                ObjectNode line = lines.addObject();
                line.put("Amount", amount);
                line.put("DetailType", "SalesItemLineDetail");
                line.put("Description", item.item != null ? item.item.itemName : "");
                ObjectNode detail = line.putObject("SalesItemLineDetail");
                detail.putObject("ItemRef").put("value", "1").put("name", "Services");
                detail.put("Qty", qty);
                detail.put("UnitPrice", unitPrice);
            }
        } else {
            ObjectNode line = lines.addObject();
            line.put("Amount", invoice.totalAmount);
            line.put("DetailType", "SalesItemLineDetail");
            line.putObject("SalesItemLineDetail").putObject("ItemRef").put("value", "1");
        }
        return node;
    }

    // ── Sync: Payments ────────────────────────────────────────────────────

    // No @Transactional — loops with HTTP calls
    public int syncAllPayments() throws Exception {
        QuickBooksConfig cfg = QuickBooksConfig.getInstance();
        List<SalesInvoice> invoices = SalesInvoice.list(
                "status = ?1 and paymentMethod != ?2",
                SalesInvoice.InvoiceStatus.DELIVERED,
                SalesInvoice.PaymentMethod.CREDIT);
        int count = 0;
        for (SalesInvoice inv : invoices) {
            QuickBooksEntityMap invoiceMap = QuickBooksEntityMap.findMapping(
                    QuickBooksEntityMap.EntityType.INVOICE, inv.id);
            if (invoiceMap == null) continue;

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

    // No @Transactional — makes HTTP call
    public void syncPayment(SalesInvoice invoice, QuickBooksEntityMap invoiceMap,
                            QuickBooksConfig cfg) throws Exception {
        Customer customer = invoice.customerBranch.customer;
        QuickBooksEntityMap customerMap = QuickBooksEntityMap.findMapping(
                QuickBooksEntityMap.EntityType.CUSTOMER, customer.id);
        if (customerMap == null) return;

        String token = getValidToken();
        ObjectNode payload = buildPaymentPayload(invoice, customerMap.qboId, invoiceMap.qboId);

        // HTTP call — no DB connection held
        JsonNode resp        = postJson(apiBase(cfg) + "payment", payload.toString(), token);
        JsonNode paymentNode = resp.get("Payment");

        // Short DB write
        mapRepo.upsertMap(QuickBooksEntityMap.EntityType.PAYMENT, invoice.id,
                paymentNode.get("Id").asText(),
                paymentNode.get("SyncToken").asText());
    }

    private ObjectNode buildPaymentPayload(SalesInvoice invoice, String qboCustomerId,
                                           String qboInvoiceId) {
        ObjectNode node = json.createObjectNode();
        node.putObject("CustomerRef").put("value", qboCustomerId);
        node.put("TotalAmt", invoice.totalAmount);

        LocalDate payDate = invoice.paymentDate != null ? invoice.paymentDate
                : (invoice.deliveryDate != null ? invoice.deliveryDate : invoice.invoiceDate);
        node.put("TxnDate", payDate.toString());

        ObjectNode lineItem = node.putArray("Line").addObject();
        lineItem.put("Amount", invoice.totalAmount);
        lineItem.putArray("LinkedTxn").addObject()
                .put("TxnId", qboInvoiceId)
                .put("TxnType", "Invoice");

        node.put("PrivateNote", "Payment via " + invoice.paymentMethod.name()
                + (invoice.paymentRef != null ? " | Ref: " + invoice.paymentRef : ""));
        return node;
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────

    private JsonNode postJson(String url, String body, String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new RuntimeException("QBO API error " + resp.statusCode() + ": " + resp.body());
        return json.readTree(resp.body());
    }

    private JsonNode postForm(String url, String body, boolean useBasicAuth) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (useBasicAuth) builder.header("Authorization", basicAuth());
        HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new RuntimeException("QBO token error " + resp.statusCode() + ": " + resp.body());
        return json.readTree(resp.body());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ── Inner types ───────────────────────────────────────────────────────

    public static class CustomerNotSyncedException extends RuntimeException {
        public CustomerNotSyncedException(String msg) { super(msg); }
    }

    public record SyncCounts(int customers, int invoices, int payments) {}
}
