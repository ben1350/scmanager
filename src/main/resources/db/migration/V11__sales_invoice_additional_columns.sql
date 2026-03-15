ALTER TABLE sales_invoice
    ADD delivery_date date;

ALTER TABLE sales_invoice
    ADD delivery_note_no VARCHAR(255);

ALTER TABLE sales_invoice
    ADD status VARCHAR(255);

ALTER TABLE sales_invoice
    ADD total_amount_ex_vat DECIMAL(12, 2);

ALTER TABLE sales_invoice
    ADD vat_amount DECIMAL(12, 2);

ALTER TABLE sales_invoice
    ALTER COLUMN status SET NOT NULL;