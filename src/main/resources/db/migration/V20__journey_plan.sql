CREATE SEQUENCE IF NOT EXISTS journey_plan_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS journey_stop_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE journey_plan
(
    id              BIGINT       NOT NULL,
    created_by      VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    name            VARCHAR(255) NOT NULL,
    assigned_rep_id BIGINT       NOT NULL,
    weekday         VARCHAR(20)  NOT NULL,
    active_flag     BOOLEAN,
    CONSTRAINT pk_journey_plan PRIMARY KEY (id)
);

CREATE TABLE journey_stop
(
    id                 BIGINT  NOT NULL,
    created_by         VARCHAR(255),
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    updated_by         VARCHAR(255),
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    journey_plan_id    BIGINT  NOT NULL,
    customer_branch_id BIGINT  NOT NULL,
    visit_order        INTEGER NOT NULL,
    CONSTRAINT pk_journey_stop PRIMARY KEY (id)
);

ALTER TABLE journey_plan
    ADD CONSTRAINT FK_JOURNEY_PLAN_ON_ASSIGNED_REP FOREIGN KEY (assigned_rep_id) REFERENCES app_user (id);

ALTER TABLE journey_stop
    ADD CONSTRAINT FK_JOURNEY_STOP_ON_PLAN FOREIGN KEY (journey_plan_id) REFERENCES journey_plan (id);

ALTER TABLE journey_stop
    ADD CONSTRAINT FK_JOURNEY_STOP_ON_BRANCH FOREIGN KEY (customer_branch_id) REFERENCES customer_branch (id);
