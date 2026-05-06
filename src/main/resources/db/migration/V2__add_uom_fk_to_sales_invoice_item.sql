ALTER TABLE sales_invoice_item
    ADD uom_id BIGINT;

ALTER TABLE sales_invoice_item
    ALTER COLUMN uom_id SET NOT NULL;

ALTER TABLE sales_invoice_item
    ADD CONSTRAINT FK_SALES_INVOICE_ITEM_ON_UOM FOREIGN KEY (uom_id) REFERENCES item_uom (id);