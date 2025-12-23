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

        const val JWT_ALG = "HmacSHA256" //TODO ECDSA
        const val CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE = "errorCode"
    }
}