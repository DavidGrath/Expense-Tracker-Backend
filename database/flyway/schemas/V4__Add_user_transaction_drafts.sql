CREATE TABLE `user_draft` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `profile_id` BIGINT NOT NULL,
    `transaction_id` BIGINT,
    `draft_type` VARCHAR(6) NOT NULL,
    `draft` JSON NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT(NOW(3)),
    FOREIGN KEY `fk_user_draft_profile` (`profile_id`) REFERENCES `profile` (`id`),
    FOREIGN KEY `fk_document_transaction` (`transaction_id`) REFERENCES `transaction` (`id`),
    CONSTRAINT `draft_type_enum` CHECK (
        `draft_type` IN (
            "create",
            "edit"
        )
    )
);