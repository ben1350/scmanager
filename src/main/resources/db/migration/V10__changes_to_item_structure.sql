ALTER TABLE item
    ADD selling_price_ex_vat DECIMAL(12, 4);

ALTER TABLE item
    ADD shelf_life_days INTEGER;

ALTER TABLE item
    ADD vat_rate DECIMAL(5, 2);