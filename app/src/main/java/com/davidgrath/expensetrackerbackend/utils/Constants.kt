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

        const val JWT_ALG = "RSA"
        const val CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE = "errorCode"
        const val ICON_NAMESPACE_MATERIAL_SYMBOLS = "materialsymbols"

    }

    public class Constraints {
        companion object {
            const val MAX_IMAGE_SIZE = 3 * 1024 * 1024L
            const val MAX_DOCUMENT_SIZE = 3 * 1024 * 1024L
            const val MAX_TRANSACTIONS_PER_PROFILE = 5_000
            const val MAX_TRANSACTION_ITEMS_PER_PROFILE = 100_000
            const val MAX_ITEMS_PER_TRANSACTION = 20
            const val MAX_IMAGES_PER_ITEM = 3
            const val MAX_DOCUMENTS_PER_TRANSACTION = 3
            const val MAX_CATEGORIES_PER_PROFILE = 30
            const val MAX_ACCOUNTS_PER_PROFILE = 5
            const val MAX_SELLERS_PER_PROFILE = 15

            const val MAX_PROFILES_PER_USER = 2
        }
    }
}