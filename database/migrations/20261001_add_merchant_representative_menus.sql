BEGIN;

CREATE TABLE merchant_representative_menus (
    merchant_id BIGINT NOT NULL REFERENCES merchant(merchant_id) ON DELETE CASCADE,
    menu_order INTEGER NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (merchant_id, menu_order)
);

COMMIT;
