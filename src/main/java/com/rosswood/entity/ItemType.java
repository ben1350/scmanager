package com.rosswood.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
/* =========================
ITEM TYPE (ENUM TABLE)
========================= */

@Entity
@Table(name = "item_type")
public class ItemType extends AuditableEntity {

    @Column(name = "item_type_code", unique = true, nullable = false)
    public String itemTypeCode; // RAW_MATERIAL, FINISHED_GOOD
}
