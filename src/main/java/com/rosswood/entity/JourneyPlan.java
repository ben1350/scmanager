package com.rosswood.entity;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * A recurring weekly visit route assigned to a sales rep by a manager.
 * The route repeats every week on {@link #weekday}; {@link #stops} are the
 * ordered customer branches the rep should visit.
 */
@Entity
@Table(name = "journey_plan")
public class JourneyPlan extends AuditableEntity {

    @Column(nullable = false)
    public String name;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_rep_id")
    public User assignedRep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public DayOfWeek weekday;

    @Column(name = "active_flag")
    public Boolean activeFlag = true;

    @OneToMany(mappedBy = "journeyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitOrder ASC")
    public List<JourneyStop> stops = new ArrayList<>();

    public void addStop(JourneyStop stop) {
        stop.journeyPlan = this;
        this.stops.add(stop);
    }

    /** Active routes a rep runs on the given weekday. */
    public static List<JourneyPlan> findByRepAndDay(Long repId, DayOfWeek day) {
        return list("assignedRep.id = ?1 and weekday = ?2 and activeFlag = true", repId, day);
    }
}
