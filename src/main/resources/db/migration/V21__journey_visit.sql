CREATE SEQUENCE IF NOT EXISTS journey_visit_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE journey_visit
(
    id              BIGINT      NOT NULL,
    created_by      VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    journey_stop_id BIGINT      NOT NULL,
    visit_date      DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    remarks         VARCHAR(1000),
    CONSTRAINT pk_journey_visit PRIMARY KEY (id)
);

ALTER TABLE journey_visit
    ADD CONSTRAINT FK_JOURNEY_VISIT_ON_STOP FOREIGN KEY (journey_stop_id) REFERENCES journey_stop (id) ON DELETE CASCADE;

-- One visit record per stop per day; also speeds up the "today's route" lookup.
CREATE UNIQUE INDEX ux_journey_visit_stop_date ON journey_visit (journey_stop_id, visit_date);
