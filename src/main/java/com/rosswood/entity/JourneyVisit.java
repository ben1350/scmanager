package com.rosswood.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Records that a sales rep visited (or deliberately skipped) a {@link JourneyStop}
 * on a given calendar day. One row per stop per day — the rep checks stops off as
 * they run the route on the mobile app.
 */
@Entity
@Table(name = "journey_visit")
public class JourneyVisit extends AuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "journey_stop_id")
    public JourneyStop journeyStop;

    @Column(name = "visit_date", nullable = false)
    public LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public VisitStatus status;

    public String remarks;

    public enum VisitStatus { VISITED, SKIPPED }

    /** The visit record for a stop on a specific day, or null if not yet touched. */
    public static JourneyVisit forStopOnDate(Long stopId, LocalDate date) {
        return find("journeyStop.id = ?1 and visitDate = ?2", stopId, date).firstResult();
    }
}
