package com.rosswood.service;

import com.rosswood.entity.QuickBooksEntityMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

/**
 * Handles all DB writes for QuickBooks entity mappings.
 * Kept in a separate bean so @Transactional is applied via the CDI proxy —
 * self-invocation within QuickBooksService would bypass Arc's interceptor.
 */
@ApplicationScoped
public class QuickBooksMapRepository {

    @Transactional
    public void upsertMap(QuickBooksEntityMap.EntityType type, Long localId,
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
}
