-- QuickBooks Online integration tables

CREATE SEQUENCE IF NOT EXISTS quickbooks_config_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS quickbooks_entity_map_seq START WITH 1 INCREMENT BY 50;

-- Stores OAuth2 credentials (singleton — always one row)
CREATE TABLE quickbooks_config (
    id            BIGINT       NOT NULL,
    realm_id      VARCHAR(50),
    access_token  TEXT,
    refresh_token TEXT,
    token_expiry  TIMESTAMP,
    sandbox       BOOLEAN      NOT NULL DEFAULT FALSE,
    connected_at  TIMESTAMP,
    connected_by  VARCHAR(255),
    CONSTRAINT pk_quickbooks_config PRIMARY KEY (id)
);

-- Maps local entity IDs to their QBO counterpart IDs
CREATE TABLE quickbooks_entity_map (
    id             BIGINT      NOT NULL,
    entity_type    VARCHAR(20) NOT NULL,   -- CUSTOMER | INVOICE | PAYMENT
    local_id       BIGINT      NOT NULL,
    qbo_id         VARCHAR(50) NOT NULL,
    qbo_sync_token VARCHAR(20),
    synced_at      TIMESTAMP,
    CONSTRAINT pk_quickbooks_entity_map PRIMARY KEY (id),
    CONSTRAINT uq_qbo_entity UNIQUE (entity_type, local_id)
);
