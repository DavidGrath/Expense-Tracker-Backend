package com.davidgrath.expensetrackerbackend.utils

class Constants {
    companion object {
        const val FOLDER_NAME_USERS = "users"
        const val FOLDER_NAME_PROFILES = "profiles"
        const val FOLDER_NAME_DATA = "data"
        const val FOLDER_NAME_DRAFT = "draft"
        const val SUBFOLDER_NAME_IMAGES = "images"
        const val SUBFOLDER_NAME_DOCUMENTS = "documents"
        const val DEFAULT_PROFILE_ID = "fae075e1-d54a-4857-a76b-b3eadf88d602"

        const val JWT_ALG = "RSA" //4096 bits
        const val CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE = "errorCode"
        const val ICON_NAMESPACE_MATERIAL_SYMBOLS = "materialsymbols"
        const val DEFAULT_FETCH_LIMIT = 1_000
        const val CUSTOM_CLAIM_TYPE = "token_type"
        const val FALSE_BYTE: Byte = 0
        const val TRUE_BYTE: Byte = 1
    }

    enum class TokenType(val value: String) {
        Access("access"),
        Refresh("refresh")
    }

    public class Constraints {
        companion object {
            const val MAX_IMAGE_SIZE = 3 * 1024 * 1024L
            const val MAX_DOCUMENT_SIZE = 3 * 1024 * 1024L
            //TODO Max user storage = 2 GB; sum(image) + sum (document) + sum (person_image) (when I add the latter)
            const val MAX_TRANSACTIONS_PER_PROFILE = 5_000
            const val MAX_TRANSACTION_ITEMS_PER_PROFILE = 100_000
            const val MAX_ITEMS_PER_TRANSACTION = 30
            const val MAX_IMAGES_PER_ITEM = 5
            const val MAX_DOCUMENTS_PER_TRANSACTION = 3
            const val MAX_CATEGORIES_PER_PROFILE = 30
            const val MAX_ACCOUNTS_PER_PROFILE = 5
            const val MAX_SELLERS_PER_PROFILE = 15
            const val MAX_LOCATIONS_PER_SELLER = 50

            const val MAX_PROFILES_PER_USER = 2
        }
    }
}