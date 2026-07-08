ALTER TABLE product_serials ADD COLUMN transfer_id BIGINT;
ALTER TABLE product_serials ADD CONSTRAINT fk_product_serials_transfer FOREIGN KEY (transfer_id) REFERENCES stock_transfer(id);
