CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` CHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3))
);

CREATE TABLE profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    string_id CHAR(36) NOT NULL COMMENT "A UUIDv4",
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_profile_user (user_id) REFERENCES `user` (id),
    UNIQUE KEY uq_user_string_id (user_id, string_id)
);

CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    string_id VARCHAR(50) COMMENT "Name of a default category",
    is_custom BOOLEAN NOT NULL,
    name VARCHAR(50),
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
    currency_code CHAR(3) NOT NULL COMMENT "An ISO 4217 currency code",
    financial_institution_id BIGINT,
    reference_number VARCHAR(50),
    name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_account_financial_institution (financial_institution_id) REFERENCES financial_institution (id),
    FOREIGN KEY fk_account_profile (profile_id) REFERENCES `profile` (id)
);

CREATE TABLE seller (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_seller_profile (profile_id) REFERENCES profile (id)
) COMMENT "An entity that sells items";

CREATE TABLE seller_location (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    location VARCHAR(255) NOT NULL COMMENT "Short, descriptive name, basically 'branch name'",
    is_virtual BOOLEAN NOT NULL,
    longitude DOUBLE,
    latitude DOUBLE,
    address VARCHAR(500) COMMENT "The kind of address you'd put in Google Maps",
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_seller_location_seller (seller_id) REFERENCES seller (id)
);

CREATE TABLE `transaction` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency_code CHAR(3) COMMENT "An ISO 4217 currency code",
    reference_number VARCHAR(50),
    debit_or_credit BOOLEAN NOT NULL,
    dated_at DATE NOT NULL,
    ordinal INT NOT NULL COMMENT "To allow for reordering of transactions",
    `mode` VARCHAR(30) NOT NULL DEFAULT "other",
    note VARCHAR(500),
    seller_id BIGINT,
    seller_location_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_account (account_id) REFERENCES account (id),
    FOREIGN KEY fk_transaction_seller (seller_id) REFERENCES seller (id),
    FOREIGN KEY fk_transaction_seller_location (seller_location_id) REFERENCES seller_location (id),
    UNIQUE KEY uq_transaction_account_dated_at_ordinal (account_id, dated_at, ordinal),
    CONSTRAINT transaction_mode_enum CHECK (
        `mode` IN (
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
    description VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    primary_category_id BIGINT NOT NULL,
    ordinal INT NOT NULL,
    quantity INT NOT NULL,
    brand VARCHAR(50),
    variation VARCHAR(20) COMMENT "Like 'XL size', or 'Chocolate flavor'",
    reference_number VARCHAR(50),
    is_reduction BOOLEAN NOT NULL COMMENT "Basically discounts for expenses and deductions for income",
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_item_transaction (transaction_id) REFERENCES transaction (id),
    FOREIGN KEY fk_transaction_item_category (primary_category_id) REFERENCES category (id),
    UNIQUE KEY uq_transaction_item_transaction_ordinal (transaction_id, ordinal)
);

CREATE TABLE `image` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha_256 CHAR(64) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    uri VARCHAR(500) NOT NULL,
    `path` VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_image_profile (profile_id) REFERENCES profile (id),
    UNIQUE KEY uq_image_profile_sha_256 (profile_id, sha_256)
);

CREATE TABLE document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL DEFAULT "other",
    size_bytes BIGINT NOT NULL,
    sha_256 CHAR(64) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    uri VARCHAR(500) NOT NULL,
    `path` VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
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

CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_product_profile (profile_id) REFERENCES profile (id)
) COMMENT "A simple template for populating items";

CREATE TABLE product_images (
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
    image_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_item_images_tranaction_item (transaction_item_id) REFERENCES transaction_item (id),
    FOREIGN KEY fk_transaction_item_images_image (image_id) REFERENCES `image` (id),
    UNIQUE KEY uq_transaction_item_images_transaction_item_image (transaction_item_id, image_id)
);

CREATE TABLE transaction_item_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_item_categories_tranaction_item (transaction_item_id) REFERENCES transaction_item (id),
    FOREIGN KEY fk_transaction_item_categories_category (category_id) REFERENCES category (id),
    UNIQUE KEY uq_transaction_item_categories_transaction_item_category (
        transaction_item_id,
        category_id
    )
);

CREATE TABLE transaction_documents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY fk_transaction_documents_transaction (transaction_id) REFERENCES `transaction` (id),
    FOREIGN KEY fk_transaction_documents_document (document_id) REFERENCES document (id),
    UNIQUE KEY uq_transaction_documents_transaction_document (transaction_id, document_id)
);