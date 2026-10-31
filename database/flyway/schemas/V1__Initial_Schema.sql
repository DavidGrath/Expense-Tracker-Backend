CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` CHAR(36) NOT NULL, --TODO change to binary 16,create a generated nonstored display column for convenience if needed
    username VARCHAR(50) NOT NULL UNIQUE,--TODO check Not blank or empty or nonsense
    password VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3))
);

CREATE TABLE profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,--TODO check Not blank or empty
    string_id CHAR(36) NOT NULL COMMENT "A UUIDv4", --TODO change name to uuid, change to binary 16,create a generated nonstored display column for convenience if needed
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_profile_user (user_id) REFERENCES `user` (id),
    UNIQUE KEY uq_user_string_id (user_id, string_id)
);

CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    string_id VARCHAR(50) COMMENT "Name of a default category",
    is_custom BOOLEAN NOT NULL, --TODO check ((is_custom and string_id is null and name is not null) or (not is_custom and string_id is not null and name is null))
    name VARCHAR(50),--TODO check is null or (Not blank or empty)
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_category_profile (profile_id) REFERENCES profile (id),
    UNIQUE KEY uq_profile_string_id (profile_id, string_id)
);

CREATE TABLE financial_institution (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_financial_institution_profile (profile_id) REFERENCES profile (id)
) COMMENT "An entity that holds accounts";

CREATE TABLE `account` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    currency_code CHAR(3) NOT NULL COMMENT "An ISO 4217 currency code",  --TODO collation ascii case sensitive, regex 3 letters uppercase
    financial_institution_id BIGINT,
    reference_number VARCHAR(50),
    name VARCHAR(50) NOT NULL, --TODO check Not blank or empty
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_account_financial_institution (financial_institution_id) REFERENCES financial_institution (id),
    FOREIGN KEY fk_account_profile (profile_id) REFERENCES `profile` (id)
);

CREATE TABLE seller (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,--TODO check Not blank or empty or nonsense
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_seller_profile (profile_id) REFERENCES profile (id)
) COMMENT "An entity that sells items";

--TODO regex for rejecting empty nonsense [^\\p{Zl}\\p{Zp}\\p{Zs}\\p{Mn}\\p{Cc}\\p{Cf}]
CREATE TABLE seller_location (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,--TODO add an ordinal column so as to avoid the diamond dependency problem on transaction
    location VARCHAR(255) NOT NULL COMMENT "Short, descriptive name, basically 'branch name'",
    is_virtual BOOLEAN NOT NULL, --TODO Delete not needed again
    longitude DOUBLE, --TODO check -90 <= x <= 90
    latitude DOUBLE, --TODO check -180 <= x <= 180
    address VARCHAR(500) COMMENT "The kind of address you'd put in Google Maps", --TODO avoid blank nonsense
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_seller_location_seller (seller_id) REFERENCES seller (id)
);

CREATE TABLE `transaction` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL, --TODO Check > 0
    currency_code CHAR(3) COMMENT "An ISO 4217 currency code", --TODO collation ascii case sensitive, regex 3 letters uppercase
    reference_number VARCHAR(50), --TODO Delete this, don't care about it again
    debit_or_credit BOOLEAN NOT NULL,
    dated_at DATE NOT NULL,
    ordinal INT NOT NULL COMMENT "To allow for reordering of transactions", --TODO ordinal is not sufficient: create occupancy_slot,sort_key, and check 1 <= occupancy_slot <= 20
    `mode` VARCHAR(30) NOT NULL DEFAULT "other",
    note VARCHAR(500), --TODO check Not blank or empty or nonsense
    seller_id BIGINT,
    seller_location_id BIGINT, --TODo diamond dependency problem, use ordinal instead
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_account (account_id) REFERENCES account (id),
    FOREIGN KEY fk_transaction_seller (seller_id) REFERENCES seller (id),
    FOREIGN KEY fk_transaction_seller_location (seller_location_id) REFERENCES seller_location (id),
    UNIQUE KEY uq_transaction_account_dated_at_ordinal (account_id, dated_at, ordinal),
    CONSTRAINT transaction_mode_enum CHECK (
        `mode` IN ( --TODO I'm reworking this soon, also collation ascii
            "card",
            "transfer",
            "cash",
            "other"
        )
    )
) COMMENT "The basis for everything else in the app";

CREATE TABLE transaction_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    description VARCHAR(50) NOT NULL, --TODO check regex not empty or blank or nonsense
    amount DECIMAL(19, 2) NOT NULL, --TODO check nonnegative
    primary_category_id BIGINT NOT NULL,
    ordinal INT NOT NULL, --TODO ordinal is not sufficient: create occupancy_slot,sort_key, and check 1 <= occupancy_slot <= 20
    quantity INT NOT NULL, --TODO check 1 <= x <= 100
    brand VARCHAR(50), --TODo check is null or if not null then not blank/empty
    variation VARCHAR(20) COMMENT "Like 'XL size', or 'Chocolate flavor'", --TODO weight, weight unit, volume, volume unit, check ((weight is null and weight unit is null) or (weight is not null and weight unit is not null)), check ((volume is null and volume unit is null) or (volume is not null and volume unit is not null))
    reference_number VARCHAR(50),--TODO Delete this, don't care about it again
    is_reduction BOOLEAN NOT NULL COMMENT "Basically discounts for expenses and deductions for income", --TODO Check constraint (NOT is_reduction OR is_reduction AND brand is null and variation is null)
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_item_transaction (transaction_id) REFERENCES transaction (id),
    FOREIGN KEY fk_transaction_item_category (primary_category_id) REFERENCES category (id),
    UNIQUE KEY uq_transaction_item_transaction_ordinal (transaction_id, ordinal)
);

CREATE TABLE `image` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    size_bytes BIGINT NOT NULL, --TODO check nonnegative
    sha_256 CHAR(64) NOT NULL, --TODO change to binary 32,create a generated nonstored display column for convenience if needed
    mime_type VARCHAR(50) NOT NULL, --TODO collation ascii case sensitive check valid regex lowercase
    uri VARCHAR(500) NOT NULL, --TODO if a regex is feasible then use it, otherwise at least ensure it starts with http(s)?
    `path` VARCHAR(500) NOT NULL, --TODO regex to start with leading slash and valid characters if pragmatic
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_image_profile (profile_id) REFERENCES profile (id),
    UNIQUE KEY uq_image_profile_sha_256 (profile_id, sha_256)
);

CREATE TABLE document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL DEFAULT "other",
    size_bytes BIGINT NOT NULL, --TODO similar changes as image above
    sha_256 CHAR(64) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    uri VARCHAR(500) NOT NULL,
    `path` VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)), --TODO create occupancy_slot,sort_key, and check 1 <= occupancy_slot <= 10
    FOREIGN KEY fk_document_transaction (transaction_id) REFERENCES `transaction` (id),
    UNIQUE KEY uq_document_transaction_sha_256 (transaction_id, sha_256),
    CONSTRAINT document_type_enum CHECK (
        `type` IN (
            "bank_statement",
            "receipt",
            "invoice",
            "other"
        )
    )
);

CREATE TABLE product ( --TODO delete, I don't care about this table again
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_product_profile (profile_id) REFERENCES profile (id)
) COMMENT "A simple template for populating items";

CREATE TABLE product_images ( --TODO delete, I don't care about this table again
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_product_images_product (product_id) REFERENCES product (id),
    FOREIGN KEY fk_product_images_image (image_id) REFERENCES `image` (id),
    UNIQUE KEY uq_product_images_product_image (product_id, image_id)
);

CREATE TABLE transaction_item_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL, --TODO create occupancy_slot,sort_key, and check 1 <= occupancy_slot <= 10
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_item_images_tranaction_item (transaction_item_id) REFERENCES transaction_item (id),
    FOREIGN KEY fk_transaction_item_images_image (image_id) REFERENCES `image` (id),
    UNIQUE KEY uq_transaction_item_images_transaction_item_image (transaction_item_id, image_id)
);

CREATE TABLE transaction_item_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)), --TODO not sure if I wanna keep this table, but if I do: create occupancy_slot and check 1 <= occupancy_slot <= 4
    FOREIGN KEY fk_transaction_item_categories_tranaction_item (transaction_item_id) REFERENCES transaction_item (id),
    FOREIGN KEY fk_transaction_item_categories_category (category_id) REFERENCES category (id),
    UNIQUE KEY uq_transaction_item_categories_transaction_item_category (
        transaction_item_id,
        category_id
    )
);

CREATE TABLE transaction_documents ( --TODo why does this table exist? Delete it
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_documents_transaction (transaction_id) REFERENCES `transaction` (id),
    FOREIGN KEY fk_transaction_documents_document (document_id) REFERENCES document (id),
    UNIQUE KEY uq_transaction_documents_transaction_document (transaction_id, document_id)
);