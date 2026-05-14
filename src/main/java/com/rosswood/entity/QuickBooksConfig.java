package com.rosswood.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Singleton row (always id=1) storing QBO OAuth2 credentials.
 * Use {@link #getInstance()} / {@link #saveInstance(QuickBooksConfig)} helpers.
 */
@Entity
@Table(name = "quickbooks_config")
public class QuickBooksConfig extends PanacheEntity {

    @Column(name = "realm_id")
    public String realmId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    public String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    public String refreshToken;

    @Column(name = "token_expiry")
    public LocalDateTime tokenExpiry;

    @Column(name = "sandbox", nullable = false)
    public boolean sandbox = false;

    @Column(name = "connected_at")
    public LocalDateTime connectedAt;

    @Column(name = "connected_by")
    public String connectedBy;

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Returns true when there is a stored connection with at least a refresh token. */
    public boolean isConnected() {
        return realmId != null && refreshToken != null;
    }

    /** Returns true when the access token is still valid (with 60 s buffer). */
    public boolean isAccessTokenValid() {
        return accessToken != null
                && tokenExpiry != null
                && LocalDateTime.now().isBefore(tokenExpiry.minusSeconds(60));
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /** Load the singleton config row; returns null if no connection has ever been saved. */
    public static QuickBooksConfig getInstance() {
        return QuickBooksConfig.find("id = 1").firstResult();
    }
}
