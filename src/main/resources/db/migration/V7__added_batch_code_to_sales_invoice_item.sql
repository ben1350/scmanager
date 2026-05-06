
ALTER TABLE sales_invoice_item
    ADD batch_code VARCHAR(255);

ALTER TABLE sales_invoice_item
    ADD line_total_ex_vat DECIMAL(12, 2);

ALTER TABLE sales_invoice_item
    ADD line_total_inc_vat DECIMAL(12, 2);

ALTER TABLE sales_invoice_item
    ADD unit_price_ex_vat DECIMAL(12, 4);

ALTER TABLE sales_invoice_item
    ADD vat_amount DECIMAL(12, 2);

ALTER TABLE sales_invoice_item
    ADD vat_rate DECIMAL(5, 2);

ALTER TABLE sales_invoice_item
    ALTER COLUMN unit_price_ex_vat SET NOT NULL;

ALTER TABLE sales_invoice_item
DROP COLUMN line_total;

ALTER TABLE sales_invoice_item
DROP COLUMN unitprice;

ALTER TABLE sales_invoice_item
ALTER COLUMN quantity TYPE DECIMAL(12, 4) USING (quantity::DECIMAL(12, 4));