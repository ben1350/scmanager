package com.rosswood.entity;

import jakarta.persistence.*;

/**
 * A single customer branch on a {@link JourneyPlan}, in visit order.
 */
@Entity
@Table(name = "journey_stop")
public class JourneyStop extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "journey_plan_id")
    public JourneyPlan journeyPlan;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_branch_id")
    public CustomerBranch customerBranch;

    @Column(name = "visit_order", nullable = false)
    public Integer visitOrder;
}
