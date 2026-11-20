CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` BINARY(16) NOT NULL COMMENT "A UUIDv7",
    `uuid_display` CHAR(36) GENERATED ALWAYS AS (bin_to_uuid(`uuid`)) VIRTUAL COMMENT 'Display value for UUID column',
    #--email VARCHAR(250) NOT NULL,
    username VARCHAR(50) NOT NULL,
    #--username VARCHAR(50), #--TODO Email and OTP table where the token is stored in AES form and cleared every minute. The email regex will be very fun (not). Username will be nullable and there'll be a new status column with a check constraint to ensure the username isn't null once the account is in certain states. Also, an FSM with a transition matrix will need to be drawn
    #--status VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    UNIQUE KEY uq_user_username (username),
    UNIQUE KEY uq_user_uuid (`uuid`),
    CONSTRAINT chk_user_username_valid CHECK (REGEXP_LIKE(`username`, '^[a-z][a-z0-9_]*[a-z0-9]$', 'c'))
    #--CONSTRAINT chk_user_email_valid CHECK (email REGEXP '^[a-zA-Z_\.0-9]+@[a-zA-Z_\.0-9]+\.[a-zA-Z_\.0-9]+'), #--TODO Test
    #--CONSTRAINT chk_user_status_enum CHECK (status in ('new', 'email_verified', 'active')), #--TODO jooq enum. I won't touch MySQL enums. There's no plan to use them
    #--CONSTRAINT chk_user_username_status CHECK ((status = 'active' AND username IS NOT NULL) or (status != 'active' AND username IS NULL)) #--TODO test
);

CREATE TABLE profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    `uuid` BINARY(16) NOT NULL COMMENT "A UUIDv7",
    `uuid_display` CHAR(36) GENERATED ALWAYS AS (bin_to_uuid(`uuid`)) VIRTUAL COMMENT 'Display value for UUID column',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering of profiles. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of profiles can exist per user. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    UNIQUE KEY uq_user_string_id (user_id, uuid),
    UNIQUE KEY uq_profile_sort_key (user_id, sort_key),
    UNIQUE KEY uq_profile_occupancy_slot (user_id, occupancy_slot),
    CONSTRAINT chk_profile_name_valid CHECK (NOT `name` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `name` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`name`) > 0),
    CONSTRAINT chk_profile_sort_key_valid CHECK (sort_key > 0), #--I could also use the unsigned variants, but that would mean dealing with 2's complement issues
    CONSTRAINT chk_profile_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 2)
);

CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    string_id VARCHAR(50) COMMENT "Name of a default category",
    is_custom BOOLEAN NOT NULL,
    `name` VARCHAR(50),
    `icon` VARCHAR(255) NOT NULL DEFAULT 'materialsymbols:category', #--TODO Maybe use a lookup table instead
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys. This ought to be more stable than sort_key or occupancy_slot',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering of cateogries within a profile. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of catefories can exist per profile. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_category_profile FOREIGN KEY (profile_id) REFERENCES profile (id),
    UNIQUE KEY uq_category_profile_string_id (profile_id, string_id),
    UNIQUE KEY uq_category_sort_ordinal (profile_id, ordinal),
    UNIQUE KEY uq_category_sort_key (profile_id, sort_key),
    UNIQUE KEY uq_category_occupancy_slot (profile_id, occupancy_slot),
    CONSTRAINT chk_category_is_custom_valid CHECK ((is_custom AND string_id IS NULL AND `name` IS NOT NULL) OR (NOT is_custom AND string_id IS NOT NULL AND `name` IS NULL)),
    CONSTRAINT chk_category_name_valid CHECK (NOT is_custom OR (`name` NOT REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `name` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$')  AND char_length(`name`) > 0),
    CONSTRAINT chk_category_icon_valid CHECK (icon REGEXP '^materialsymbols\\:[a-z0-9_]+$'),
    CONSTRAINT chk_category_ordinal_valid CHECK (ordinal > 0),
    CONSTRAINT chk_category_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_category_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 50)
);

CREATE TABLE financial_institution (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_financial_institution_profile FOREIGN KEY (profile_id) REFERENCES profile (id)
) COMMENT "An entity that holds accounts";

CREATE TABLE `account` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    currency_code CHAR(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT "An ISO 4217 currency code",
    financial_institution_id BIGINT,
    reference_number VARCHAR(50),
    `name` VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL,
    default_indicator BIGINT GENERATED ALWAYS AS (IF (is_default, profile_id, null)) STORED NULL COMMENT 'To ensure at most 1 default account is present per profile',
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys. This ought to be more stable than sort_key or occupancy_slot',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering of accounts within a profile. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of accounts can exist per profile. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_account_financial_institution FOREIGN KEY (financial_institution_id) REFERENCES financial_institution (id),
    CONSTRAINT fk_account_profile FOREIGN KEY (profile_id) REFERENCES `profile` (id),
    UNIQUE KEY uq_account_ordinal (profile_id, ordinal),
    UNIQUE KEY uq_account_occupancy_slot (profile_id, occupancy_slot),
    UNIQUE KEY uq_account_sort_key (profile_id, sort_key),
    UNIQUE KEY uq_account_default_indicator (profile_id, default_indicator),
    CONSTRAINT chk_account_currency_code_valid CHECK (REGEXP_LIKE(currency_code, '[A-Z]{3}', 'c')),
    CONSTRAINT chk_account_name_valid CHECK (`name` NOT REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `name` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`name`) > 0),
    CONSTRAINT chk_account_ordinal_valid CHECK (ordinal > 0),
    CONSTRAINT chk_account_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_account_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 10)
);

CREATE TABLE seller (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys. This ought to be more stable than sort_key or occupancy_slot',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of sellers can exist per profile. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_seller_profile FOREIGN KEY (profile_id) REFERENCES profile (id),
    UNIQUE KEY uq_seller_ordinal (profile_id, ordinal),
    UNIQUE KEY uq_seller_sort_key (profile_id, sort_key),
    UNIQUE KEY uq_seller_occupancy_slot (profile_id, occupancy_slot),
    CONSTRAINT chk_seller_name_valid CHECK (NOT `name` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `name` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`name`) > 0),
    CONSTRAINT chk_seller_ordinal_valid CHECK (ordinal > 0),
    CONSTRAINT chk_seller_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_seller_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 15)
) COMMENT "An entity that a user makes transactions with. 'twill be a fun time when I add persons to this schema";

CREATE TABLE seller_location (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    seller_ordinal SMALLINT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    longitude DOUBLE,
    latitude DOUBLE,
    address VARCHAR(500) COMMENT "The kind of address you'd put in Google Maps",
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys. This ought to be more stable than sort_key or occupancy_slot',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of locations can exist per seller. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_seller_location_seller FOREIGN KEY (profile_id, seller_ordinal) REFERENCES seller (profile_id, ordinal),
    UNIQUE KEY uq_seller_location_seller_ordinal (profile_id, seller_ordinal, `ordinal`),
    UNIQUE KEY uq_seller_location_seller_sort_key (profile_id, seller_ordinal, `sort_key`),
    UNIQUE KEY uq_seller_location_seller_occupancy_slot (profile_id, seller_ordinal, `occupancy_slot`),
    CONSTRAINT chk_seller_location_name_valid CHECK (NOT `name` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `name` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`name`) > 0),
    CONSTRAINT chk_seller_location_longitude_valid CHECK (longitude IS NULL OR (-180 <= longitude AND longitude <= 180)),
    CONSTRAINT chk_seller_location_latitude_valid CHECK (latitude IS NULL OR (-90 <= latitude AND latitude <= 90)),
    CONSTRAINT chk_seller_location_coordinates_valid CHECK ((longitude IS NULL AND latitude IS NULL) OR (longitude IS NOT NULL AND latitude IS NOT NULL)),
    CONSTRAINT chk_seller_location_address_valid CHECK (`address` IS NULL OR (NOT `address` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `address` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`address`) > 0)),
    CONSTRAINT chk_seller_location_ordinal_valid CHECK (ordinal > 0),
    CONSTRAINT chk_seller_location_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_seller_location_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 50)
);

CREATE TABLE `transaction` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL COMMENT 'Equal to the sum of the amount of non-reduction items minus the sum of reduction items',
    debit_or_credit BOOLEAN NOT NULL,
    dated_at DATE NOT NULL,
    `time` TIME COMMENT 'If present, used to determine the sort_key position of a transaction within a day for an account',
    `mode` VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT "other",
    note VARCHAR(500),
    account_ordinal SMALLINT NOT NULL,
    seller_ordinal SMALLINT,
    seller_location_ordinal SMALLINT,
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys. This ought to be more stable than sort_key or occupancy_slot',
    sort_key SMALLINT NOT NULL COMMENT "To allow for custom reordering of transactions. WARNING: Not to be relied on as a stable secondary identifier",
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of transactions can exist per day per account. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_transaction_account FOREIGN KEY (profile_id, account_ordinal) REFERENCES account (profile_id, ordinal),
    CONSTRAINT fk_transaction_seller FOREIGN KEY (profile_id, seller_ordinal) REFERENCES seller (profile_id, ordinal),
    CONSTRAINT fk_transaction_seller_location FOREIGN KEY (profile_id, seller_ordinal, seller_location_ordinal) REFERENCES seller_location (profile_id, seller_ordinal, ordinal),
    UNIQUE KEY uq_transaction_account_ordinal (profile_id, account_ordinal, ordinal),
    UNIQUE KEY uq_transaction_account_dated_at_sort_key (profile_id, account_ordinal, dated_at, sort_key),
    UNIQUE KEY uq_transaction_account_dated_at_occupancy_slot (profile_id, account_ordinal, dated_at, occupancy_slot),
    CONSTRAINT chk_transaction_mode_enum CHECK (
        `mode` IN ( #--TODO Im reworking this soon
            "card",
            "transfer",
            "cash",
            "other"
        )
    ),
    CONSTRAINT chk_transaction_amount_valid CHECK (0 < amount AND amount <= 100000000),
    CONSTRAINT chk_transaction_seller_location_ordinal_valid CHECK ((seller_ordinal IS NULL AND seller_location_ordinal IS NULL) OR (seller_ordinal IS NOT NULL AND seller_location_ordinal IS NULL) or (seller_ordinal IS NOT NULL AND seller_location_ordinal IS NOT NULL)),
    CONSTRAINT chk_transaction_note_valid CHECK (`note` IS NULL OR NOT `note` REGEXP '[\\p{Cc}--[\\r\\n\\t]]' AND NOT `note` REGEXP '^[\\p{Zl}\\p{Zp}\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`note`) > 0),
    CONSTRAINT chk_transaction_ordinal_valid CHECK (ordinal > 0),
    CONSTRAINT chk_transaction_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_transaction_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 1000)
) COMMENT "The basis for everything else in the app";

CREATE TABLE transaction_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    account_ordinal SMALLINT NOT NULL,
    transaction_ordinal SMALLINT NOT NULL,
    description VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    primary_category_ordinal SMALLINT NOT NULL, #--TODO Copy the pathfinding query over
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys. This ought to be more stable than sort_key or occupancy_slot',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering of items within transactions. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of items can exist per transaction. WARNING: Not to be relied on as a stable secondary identifier',
    quantity INT NOT NULL,
    brand VARCHAR(50),
    variation VARCHAR(20) COMMENT "Like 'XL size', or 'Chocolate flavor'",
    weight DECIMAL(12,6),
    weight_prefix VARCHAR(2) CHARACTER SET ascii COLLATE ascii_bin COMMENT "For metric units only",
    weight_unit VARCHAR(20),
    volume DECIMAL(12,6),
    volume_prefix VARCHAR (2) CHARACTER SET ascii COLLATE ascii_bin COMMENT "For metric units only",
    volume_unit VARCHAR(20),
    is_reduction BOOLEAN NOT NULL COMMENT "Basically discounts for expenses and deductions for income",
    is_charge BOOLEAN NOT NULL COMMENT "General flag for things like fees, charges, and taxes",
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_transaction_item_transaction FOREIGN KEY (profile_id, account_ordinal, transaction_ordinal) REFERENCES transaction (profile_id, account_ordinal, ordinal),
    CONSTRAINT fk_transaction_item_category FOREIGN KEY (profile_id, primary_category_ordinal) REFERENCES category (profile_id, ordinal),
    UNIQUE KEY uq_transaction_item_transaction_ordinal (profile_id, account_ordinal, transaction_ordinal, ordinal),
    UNIQUE KEY uq_transaction_item_transaction_sort_key (profile_id, account_ordinal, transaction_ordinal,  sort_key),
    UNIQUE KEY uq_transaction_item_transaction_occupancy_slot (profile_id, account_ordinal, transaction_ordinal,  occupancy_slot),
    CONSTRAINT chk_transaction_item_amount_valid CHECK (0 < amount AND amount <= 100000000),
    CONSTRAINT chk_transaction_item_description_valid CHECK (NOT `description` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `description` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`description`) > 0),
    CONSTRAINT chk_transaction_item_brand_valid CHECK (brand IS NULL OR (NOT `brand` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `brand` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`brand`) > 0)),
    CONSTRAINT chk_transaction_item_variation_valid CHECK (variation IS NULL OR (NOT `variation` REGEXP '[\\p{Cc}\\p{Zl}\\p{Zp}]' AND NOT `variation` REGEXP '^[\\p{Zs}\\p{M}\\p{Cf}]+$' AND char_length(`variation`) > 0)),
    CONSTRAINT chk_transaction_item_quantity_valid CHECK (1 <= quantity AND quantity <= 100),
    CONSTRAINT chk_transaction_item_is_reduction_valid CHECK (NOT is_reduction OR (is_reduction AND brand IS NULL and variation IS NULL AND weight IS NULL AND weight_prefix IS NULL AND weight_unit IS NULL AND volume IS NULL AND volume_prefix AND volume_unit IS NULL AND quantity=1)),
    CONSTRAINT chk_transaction_item_weight_valid CHECK ((weight IS NULL and weight_unit IS NULL) or (weight IS NOT NULL AND weight_unit IS NOT NULL AND weight > 0)),
    CONSTRAINT chk_transaction_item_volume_valid CHECK ((volume IS NULL and volume_unit IS NULL) or (volume IS NOT NULL AND volume_unit IS NOT NULL AND volume > 0)),
    CONSTRAINT chk_transaction_item_weight_unit_valid CHECK (weight_unit IS NULL OR weight_unit IN ('g', '[oz_av]', '[lb_av]')),
    CONSTRAINT chk_transaction_item_volume_unit_valid CHECK (volume_unit IS NULL OR volume_unit IN ('l', '[foz_us]', '[gal_us]')),
    CONSTRAINT chk_transaction_item_weight_prefix_valid CHECK (weight_prefix IS NULL OR (weight_unit = 'g' AND weight_prefix IN ('m', 'c', 'k', 'M')) OR (weight_unit != 'g' AND weight_prefix IS NULL)),
    CONSTRAINT chk_transaction_item_volume_prefix_valid CHECK (volume_prefix IS NULL OR (volume_unit = 'l' AND volume_prefix IN ('m', 'c', 'k', 'M')) OR (volume_unit != 'l' AND volume_prefix IS NULL)),
    CONSTRAINT chk_transaction_item_reduction_charge_valid CHECK (NOT (is_reduction AND is_charge)), #--TODO test
    CONSTRAINT chk_transaction_item_ordinal_valid CHECK (ordinal > 0),
    CONSTRAINT chk_transaction_item_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_transaction_item_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 30)
);

CREATE TABLE `image` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha_256 BINARY(32) NOT NULL COMMENT 'Primarily to prevent unnecessary duplication during uploads',
    `sha_256_display` CHAR(64) GENERATED ALWAYS AS (hex(`sha_256`)) VIRTUAL COMMENT 'Display value for SHA 256 column',
    mime_type VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    uri VARCHAR(500) NOT NULL, #--TODO delete: the uri should always be derivable from the path and so this column is redundant
    `path` VARCHAR(500) NOT NULL COMMENT 'Relative path from filesystem root. Ideally it shouldnt change between storage provicers',
    `ordinal` SMALLINT NOT NULL COMMENT 'For use with composite foreign keys', #--Wouldn't make sense to have a sort key here since images are primarily attached to items through the junction table. That one actually has a sort key
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_image_profile FOREIGN KEY (profile_id) REFERENCES profile (id),
    UNIQUE KEY uq_image_profile_ordinal (profile_id, ordinal),
    UNIQUE KEY uq_image_profile_sha_256 (profile_id, sha_256),
    CONSTRAINT chk_image_size_bytes_valid CHECK (size_bytes > 0),
    CONSTRAINT chk_image_mime_type_valid CHECK (REGEXP_LIKE(`mime_type`, '^[a-z]+/[a-z]+$', 'c')),
    CONSTRAINT chk_image_sha_256_valid CHECK (sha_256 != x'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'),
    CONSTRAINT chk_image_path_valid CHECK (`path` NOT REGEXP '[\\p{Cc}\\\\<>\\|\\:\\"\\?\\*]' AND `path` NOT REGEXP '%[a-f0-9]{2}' AND `path` NOT REGEXP '(^/|/$)' AND `path` NOT REGEXP '^\\.\\.?' AND `path` NOT REGEXP '/\\.\\.?$' AND `path` NOT REGEXP '/\\.\\.?/' AND NOT `path` REGEXP '//' AND `path` NOT REGEXP '^\\.\\.?$' AND char_length(`path`) > 0),
    CONSTRAINT chk_image_ordinal_valid CHECK (ordinal > 0)
);

CREATE TABLE document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL DEFAULT "other",
    size_bytes BIGINT NOT NULL,
    sha_256 BINARY(32) NOT NULL COMMENT 'Primarily to prevent unnecessary duplication during uploads',
    `sha_256_display` CHAR(64) GENERATED ALWAYS AS (hex(`sha_256`)) VIRTUAL COMMENT 'Display value for SHA 256 column',
    mime_type VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    uri VARCHAR(500) NOT NULL, #--TODO same as above
    `path` VARCHAR(500) NOT NULL COMMENT 'Relative path from filesystem root. Ideally it shouldnt change between storage providers',
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering of documents within transactions. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of documents can exist per transaction. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_document_transaction FOREIGN KEY (transaction_id) REFERENCES `transaction` (id),
    UNIQUE KEY uq_document_transaction_sha_256 (transaction_id, sha_256),
    UNIQUE KEY uq_document_sort_key (transaction_id, sort_key),
    UNIQUE KEY uq_document_occupancy_slot (transaction_id, occupancy_slot),
    CONSTRAINT chk_document_type_enum CHECK (
        `type` IN (
            "bank_statement",
            "receipt",
            "invoice",
            "other"
        )
    ),
    CONSTRAINT chk_document_size_bytes_valid CHECK (size_bytes > 0),
    CONSTRAINT chk_document_mime_type_valid CHECK (REGEXP_LIKE(`mime_type`, '^[a-z]+/[a-z]+$', 'c')),
    CONSTRAINT chk_document_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_document_sha_256_valid CHECK (sha_256 != x'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'),
    CONSTRAINT chk_document_path_valid CHECK (`path` NOT REGEXP '[\\p{Cc}\\\\<>\\|\\:\\"\\?\\*]' AND `path` NOT REGEXP '%[a-f0-9]{2}' AND `path` NOT REGEXP '(^/|/$)' AND `path` NOT REGEXP '^\\.\\.?' AND `path` NOT REGEXP '/\\.\\.?$' AND `path` NOT REGEXP '/\\.\\.?/' AND NOT `path` REGEXP '//' AND `path` NOT REGEXP '^\\.\\.?$' AND char_length(`path`) > 0),
    CONSTRAINT chk_document_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 10)
) COMMENT 'Distinct from image in that documents are tied to individual transactions, while images are reusable across items';


CREATE TABLE transaction_item_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    account_ordinal SMALLINT NOT NULL,
    transaction_ordinal SMALLINT NOT NULL,
    transaction_item_ordinal SMALLINT NOT NULL,
    image_ordinal SMALLINT NOT NULL,
    sort_key SMALLINT NOT NULL COMMENT 'To allow for custom reordering of images within transaction items. WARNING: Not to be relied on as a stable secondary identifier',
    occupancy_slot SMALLINT NOT NULL COMMENT 'To ensure a maximum number of items can exist per transaction. WARNING: Not to be relied on as a stable secondary identifier',
    created_at DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT fk_transaction_item_images_tranaction_item FOREIGN KEY (profile_id, account_ordinal, transaction_ordinal, transaction_item_ordinal) REFERENCES transaction_item (profile_id, account_ordinal, transaction_ordinal, ordinal),
    CONSTRAINT fk_transaction_item_images_image FOREIGN KEY (profile_id, image_ordinal) REFERENCES `image` (profile_id, ordinal),
    UNIQUE KEY uq_transaction_item_images_transaction_item_image (profile_id, account_ordinal, transaction_ordinal, transaction_item_ordinal, image_ordinal),
    UNIQUE KEY uq_transaction_item_images_transaction_item_sort_key (profile_id, account_ordinal, transaction_ordinal, transaction_item_ordinal, sort_key),
    UNIQUE KEY uq_transaction_item_images_transaction_item_occupancy_slot (profile_id, account_ordinal, transaction_ordinal, transaction_item_ordinal, occupancy_slot),
    CONSTRAINT chk_transaction_item_images_category_sort_key_valid CHECK (sort_key > 0),
    CONSTRAINT chk_transaction_item_images_occupancy_slot_valid CHECK (1 <= occupancy_slot AND occupancy_slot <= 5)
);

CREATE TABLE transaction_item_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT(NOW(3)), #--TODO not sure if I wanna keep this table, but if I do: create occupancy_slot and check 1 <= occupancy_slot <= 4
    CONSTRAINT fk_transaction_item_categories_tranaction_item FOREIGN KEY (transaction_item_id) REFERENCES transaction_item (id),
    CONSTRAINT fk_transaction_item_categories_category FOREIGN KEY (category_id) REFERENCES category (id),
    UNIQUE KEY uq_transaction_item_categories_transaction_item_category (
        transaction_item_id,
        category_id
    )
);

CREATE TABLE `user_draft` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `profile_id` BIGINT NOT NULL,
    account_ordinal SMALLINT,
    transaction_ordinal SMALLINT,
    `draft_type` VARCHAR(6) NOT NULL,
    `draft` JSON NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT `fk_user_draft_profile` FOREIGN KEY (`profile_id`) REFERENCES `profile` (`id`),
    CONSTRAINT `fk_user_draft_transaction` FOREIGN KEY (profile_id, account_ordinal, `transaction_ordinal`) REFERENCES `transaction` (profile_id, account_ordinal, `ordinal`),
    UNIQUE KEY uq_user_draft_type (profile_id, draft_type),
    CONSTRAINT `draft_type_enum` CHECK (
        `draft_type` IN (
            "create",
            "edit"
        )
    ),
    CONSTRAINT chk_user_draft_is_for_transaction_is_valid CHECK ((draft_type='create' AND (account_ordinal IS NULL AND transaction_ordinal IS NULL)) OR (draft_type='edit' AND (account_ordinal IS NOT NULL AND transaction_ordinal IS NOT NULL))) #--TODO untested
);