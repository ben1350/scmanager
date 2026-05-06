ALTER TABLE stock_transaction
    ADD batch_code VARCHAR(255);

ALTER TABLE stock_transaction
    ADD direction VARCHAR(255);

ALTER TABLE stock_transaction
    ADD expiry_date date;

ALTER TABLE stock_transaction
    ADD production_batch_id BIGINT;

ALTER TABLE stock_transaction
    ADD sales_invoice_id BIGINT;

ALTER TABLE stock_transaction
    ADD uom_code VARCHAR(255);

ALTER TABLE stock_transaction
    ALTER COLUMN direction SET NOT NULL;

CREATE INDEX idx_stx_date ON stock_transaction (transaction_date);

CREATE INDEX idx_stx_item ON stock_transaction (item_id);

CREATE INDEX idx_stx_lot ON stock_transaction (batch_code);

ALTER TABLE stock_transaction
    ADD CONSTRAINT FK_STOCK_TRANSACTION_ON_PRODUCTION_BATCH FOREIGN KEY (production_batch_id) REFERENCES production_batch (id);

ALTER TABLE stock_transaction
    ADD CONSTRAINT FK_STOCK_TRANSACTION_ON_SALES_INVOICE FOREIGN KEY (sales_invoice_id) REFERENCES sales_invoice (id);

ALTER TABLE stock_transaction
ALTER
COLUMN quantity TYPE DECIMAL(12, 4) USING (quantity::DECIMAL(12, 4));