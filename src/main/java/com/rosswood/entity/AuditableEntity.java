package com.rosswood.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class AuditableEntity extends PanacheEntity {

    @Column(name = "created_by", updatable = false)
    public String createdBy;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // If using security, you'd get the username here
        // this.updatedBy = SecurityIdentity.getPrincipal().getName();
    }
}
