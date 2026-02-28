CREATE SEQUENCE IF NOT EXISTS customer_branch_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS item_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS item_type_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS item_uom_conversion_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS item_uom_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS production_batch_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS production_consumption_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS sales_invoice_item_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS sales_invoice_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS stock_opening_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS stock_transaction_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS unit_of_measure_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE customer
(
    id            BIGINT       NOT NULL,
    created_by    VARCHAR(255),
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    updated_by    VARCHAR(255),
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    customer_code VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    address       VARCHAR(255),
    email         VARCHAR(255),
    phone         VARCHAR(255),
    CONSTRAINT pk_customer PRIMARY KEY (id)
);

CREATE TABLE customer_branch
(
    id            BIGINT       NOT NULL,
    created_by    VARCHAR(255),
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    updated_by    VARCHAR(255),
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    customer_id   BIGINT       NOT NULL,
    branch_name   VARCHAR(255) NOT NULL,
    branchAddress VARCHAR(255),
    contactPerson VARCHAR(255),
    contactPhone  VARCHAR(255),
    active_flag   BOOLEAN,
    CONSTRAINT pk_customer_branch PRIMARY KEY (id)
);

CREATE TABLE item
(
    id           BIGINT       NOT NULL,
    created_by   VARCHAR(255),
    created_at   TIMESTAMP WITHOUT TIME ZONE,
    updated_by   VARCHAR(255),
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    item_code    VARCHAR(255) NOT NULL,
    item_name    VARCHAR(255) NOT NULL,
    item_type_id BIGINT       NOT NULL,
    uom_id       BIGINT       NOT NULL,
    active_flag  BOOLEAN,
    CONSTRAINT pk_item PRIMARY KEY (id)
);

CREATE TABLE item_type
(
    id             BIGINT       NOT NULL,
    created_by     VARCHAR(255),
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    updated_by     VARCHAR(255),
    updated_at     TIMESTAMP WITHOUT TIME ZONE,
    item_type_code VARCHAR(255) NOT NULL,
    CONSTRAINT pk_item_type PRIMARY KEY (id)
);

CREATE TABLE item_uom
(
    id         BIGINT       NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    item_id    BIGINT       NOT NULL,
    uom_code   VARCHAR(255) NOT NULL,
    is_base    BOOLEAN      NOT NULL,
    CONSTRAINT pk_item_uom PRIMARY KEY (id)
);

CREATE TABLE item_uom_conversion
(
    id                BIGINT       NOT NULL,
    created_by        VARCHAR(255),
    created_at        TIMESTAMP WITHOUT TIME ZONE,
    updated_by        VARCHAR(255),
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    item_id           BIGINT       NOT NULL,
    from_uom          VARCHAR(255) NOT NULL,
    to_uom            VARCHAR(255) NOT NULL,
    conversion_factor DECIMAL(12, 4),
    CONSTRAINT pk_item_uom_conversion PRIMARY KEY (id)
);

CREATE TABLE production_batch
(
    id               BIGINT       NOT NULL,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    updated_by       VARCHAR(255),
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    batch_code       VARCHAR(255) NOT NULL,
    production_date  date         NOT NULL,
    finished_item_id BIGINT       NOT NULL,
    output_qty       DECIMAL(12, 2),
    remarks          VARCHAR(255),
    status           VARCHAR(255),
    CONSTRAINT pk_production_batch PRIMARY KEY (id)
);

CREATE TABLE production_consumption
(
    id           BIGINT         NOT NULL,
    created_by   VARCHAR(255),
    created_at   TIMESTAMP WITHOUT TIME ZONE,
    updated_by   VARCHAR(255),
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    batch_id     BIGINT         NOT NULL,
    raw_item_id  BIGINT         NOT NULL,
    consumed_qty DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_production_consumption PRIMARY KEY (id)
);

CREATE TABLE sales_invoice
(
    id                  BIGINT       NOT NULL,
    created_by          VARCHAR(255),
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    updated_by          VARCHAR(255),
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    rosswood_invoice_no VARCHAR(255) NOT NULL,
    vat_invoice_no      VARCHAR(255),
    customer_branch_id  BIGINT       NOT NULL,
    invoice_date        date         NOT NULL,
    total_amount        DECIMAL(12, 2),
    remarks             VARCHAR(255),
    CONSTRAINT pk_sales_invoice PRIMARY KEY (id)
);

CREATE TABLE sales_invoice_item
(
    id         BIGINT         NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    invoice_id BIGINT         NOT NULL,
    item_id    BIGINT         NOT NULL,
    quantity   DECIMAL(12, 2) NOT NULL,
    unitPrice  DECIMAL(12, 2) NOT NULL,
    line_total DECIMAL(12, 2),
    CONSTRAINT pk_sales_invoice_item PRIMARY KEY (id)
);

CREATE TABLE stock_opening
(
    id          BIGINT         NOT NULL,
    created_by  VARCHAR(255),
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_by  VARCHAR(255),
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    stock_date  date           NOT NULL,
    item_id     BIGINT         NOT NULL,
    opening_qty DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_stock_opening PRIMARY KEY (id)
);

CREATE TABLE stock_transaction
(
    id               BIGINT         NOT NULL,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    updated_by       VARCHAR(255),
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    transaction_date date           NOT NULL,
    item_id          BIGINT         NOT NULL,
    transaction_type VARCHAR(255)   NOT NULL,
    quantity         DECIMAL(12, 2) NOT NULL,
    reference_no     VARCHAR(255),
    remarks          VARCHAR(255),
    CONSTRAINT pk_stock_transaction PRIMARY KEY (id)
);

CREATE TABLE unit_of_measure
(
    id          BIGINT       NOT NULL,
    created_by  VARCHAR(255),
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_by  VARCHAR(255),
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    uom_code    VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_unit_of_measure PRIMARY KEY (id)
);

ALTER TABLE stock_opening
    ADD CONSTRAINT uc_abab1f96390d0d5ef53110139 UNIQUE (stock_date, item_id);

ALTER TABLE customer
    ADD CONSTRAINT uc_customer_customer_code UNIQUE (customer_code);

ALTER TABLE item
    ADD CONSTRAINT uc_item_item_code UNIQUE (item_code);

ALTER TABLE item_type
    ADD CONSTRAINT uc_item_type_item_type_code UNIQUE (item_type_code);

ALTER TABLE production_batch
    ADD CONSTRAINT uc_production_batch_batch_code UNIQUE (batch_code);

ALTER TABLE sales_invoice
    ADD CONSTRAINT uc_sales_invoice_rosswood_invoice_no UNIQUE (rosswood_invoice_no);

ALTER TABLE unit_of_measure
    ADD CONSTRAINT uc_unit_of_measure_uom_code UNIQUE (uom_code);

ALTER TABLE customer_branch
    ADD CONSTRAINT FK_CUSTOMER_BRANCH_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES customer (id);

ALTER TABLE item
    ADD CONSTRAINT FK_ITEM_ON_ITEM_TYPE FOREIGN KEY (item_type_id) REFERENCES item_type (id);

ALTER TABLE item
    ADD CONSTRAINT FK_ITEM_ON_UOM FOREIGN KEY (uom_id) REFERENCES unit_of_measure (id);

ALTER TABLE item_uom_conversion
    ADD CONSTRAINT FK_ITEM_UOM_CONVERSION_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item_uom
    ADD CONSTRAINT FK_ITEM_UOM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE production_batch
    ADD CONSTRAINT FK_PRODUCTION_BATCH_ON_FINISHED_ITEM FOREIGN KEY (finished_item_id) REFERENCES item (id);

ALTER TABLE production_consumption
    ADD CONSTRAINT FK_PRODUCTION_CONSUMPTION_ON_BATCH FOREIGN KEY (batch_id) REFERENCES production_batch (id);

ALTER TABLE production_consumption
    ADD CONSTRAINT FK_PRODUCTION_CONSUMPTION_ON_RAW_ITEM FOREIGN KEY (raw_item_id) REFERENCES item (id);

ALTER TABLE sales_invoice_item
    ADD CONSTRAINT FK_SALES_INVOICE_ITEM_ON_INVOICE FOREIGN KEY (invoice_id) REFERENCES sales_invoice (id);

ALTER TABLE sales_invoice_item
    ADD CONSTRAINT FK_SALES_INVOICE_ITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE sales_invoice
    ADD CONSTRAINT FK_SALES_INVOICE_ON_CUSTOMER_BRANCH FOREIGN KEY (customer_branch_id) REFERENCES customer_branch (id);

ALTER TABLE stock_opening
    ADD CONSTRAINT FK_STOCK_OPENING_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE stock_transaction
    ADD CONSTRAINT FK_STOCK_TRANSACTION_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);