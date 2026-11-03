CREATE TABLE `user_draft` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `profile_id` BIGINT NOT NULL,
    `transaction_id` BIGINT, #TODO Diamond Dependency problem; make it (profile_id,account_ordinal,transaction_ordinal), check (account_ordinal is null and transaction_ordinal is null) or (account_ordinal is not null and transaction_ordinal is not null)
    `draft_type` VARCHAR(6) NOT NULL,
    `draft` JSON NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT(NOW(3)),
    CONSTRAINT `fk_user_draft_profile` FOREIGN KEY (`profile_id`) REFERENCES `profile` (`id`),
    CONSTRAINT `fk_user_draft_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`id`),
    CONSTRAINT `draft_type_enum` CHECK (
        `draft_type` IN (
            "create",
            "edit"
        )
    )
);