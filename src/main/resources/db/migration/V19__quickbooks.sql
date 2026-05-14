-- QuickBooks Online integration tables

-- Stores OAuth2 credentials (singleton — always one row, id=1)
CREATE TABLE quickbooks_config (
    id            BIGSERIAL PRIMARY KEY,
    realm_id      VARCHAR(50),
    access_token  TEXT,
    refresh_token TEXT,
    token_expiry  TIMESTAMP,
    sandbox       BOOLEAN NOT NULL DEFAULT FALSE,
    connected_at  TIMESTAMP,
    connected_by  VARCHAR(255)
);

-- Maps local entity IDs to their QBO counterpart IDs
CREATE TABLE quickbooks_entity_map (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(20)  NOT NULL,   -- CUSTOMER | INVOICE | PAYMENT
    local_id      BIGINT       NOT NULL,
    qbo_id        VARCHAR(50)  NOT NULL,
    qbo_sync_token VARCHAR(20),
    synced_at     TIMESTAMP,
    CONSTRAINT uq_qbo_entity UNIQUE (entity_type, local_id)
);
