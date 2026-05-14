package com.rosswood.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Maps a local entity (Customer / SalesInvoice / Payment) to its QBO counterpart.
 * One row per synced object — updated on re-sync.
 */
@Entity
@Table(name = "quickbooks_entity_map",
       uniqueConstraints = @UniqueConstraint(columnNames = {"entity_type", "local_id"}))
public class QuickBooksEntityMap extends PanacheEntity {

    public enum EntityType { CUSTOMER, INVOICE, PAYMENT }

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    public EntityType entityType;

    @Column(name = "local_id", nullable = false)
    public Long localId;

    @Column(name = "qbo_id", nullable = false, length = 50)
    public String qboId;

    /** QBO concurrency token — required when updating an existing QBO entity. */
    @Column(name = "qbo_sync_token", length = 20)
    public String qboSyncToken;

    @Column(name = "synced_at")
    public LocalDateTime syncedAt;

    // ── Queries ───────────────────────────────────────────────────────────

    public static QuickBooksEntityMap findMapping(EntityType type, Long localId) {
        return find("entityType = ?1 and localId = ?2", type, localId).firstResult();
    }

    public static long countSynced(EntityType type) {
        return count("entityType = ?1", type);
    }
}
