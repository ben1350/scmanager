-- Link a sales invoice to the route visit that produced it (Option B).
-- Nullable: most invoices are raised outside the route flow and stay unlinked.
-- ON DELETE SET NULL so a rep clearing a visit (which deletes the JourneyVisit)
-- never fails on the FK and never destroys the invoice — it just drops the link.
ALTER TABLE sales_invoice ADD COLUMN journey_visit_id BIGINT;

ALTER TABLE sales_invoice
    ADD CONSTRAINT fk_sales_invoice_on_journey_visit
    FOREIGN KEY (journey_visit_id) REFERENCES journey_visit (id)
    ON DELETE SET NULL;
