package com.rosswood.entity;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class AuditableEntity extends PanacheEntity {

    @Column(name = "created_by", updatable = false)
    public String createdBy;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_by")
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onPersist() {
        this.createdAt = LocalDateTime.now();
        this.createdBy = currentUser();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = currentUser();
    }

    private static String currentUser() {
        try {
            InjectableInstance<SecurityIdentity> instance =
                    Arc.container().select(SecurityIdentity.class);
            if (instance.isResolvable()) {
                SecurityIdentity identity = instance.get();
                if (identity != null && !identity.isAnonymous()) {
                    return identity.getPrincipal().getName();
                }
            }
        } catch (Exception ignored) {
            // Not in a request context (startup, migration, tests)
        }
        return null;
    }
}
