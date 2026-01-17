package com.rosswood.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/* =========================
UNIT OF MEASURE
========================= */

@Entity
@Table(name = "unit_of_measure")
public class UnitOfMeasure extends AuditableEntity {

    @Column(name = "uom_code", unique = true, nullable = false)
    public String uomCode;

    @Column(name = "description", nullable = false)
    public String description;

    public UnitOfMeasure() {}

    public UnitOfMeasure(String uomCode, String description) {
        this.uomCode = uomCode;
        this.description = description;
    }
}
