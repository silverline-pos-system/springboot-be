ALTER TABLE product_serials ADD COLUMN serial_no_suffix VARCHAR(100);
CREATE INDEX idx_product_serials_suffix ON product_serials(serial_no_suffix);
UPDATE product_serials SET serial_no_suffix = RIGHT(serial_no, 9) WHERE serial_no IS NOT NULL;
