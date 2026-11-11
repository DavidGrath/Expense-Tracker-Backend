package com.davidgrath.expensetrackerbackend.testutils

class TestConstants {
    companion object {
        const val DEFAULT_USER_ID = "959d8167-4501-43f1-9330-d2090c3f3cb3"

        /**
         * This isn't a main constant that would be used to initialise the profile of a user because ideally the initial account name is populated based on the locale of the user
         */
        const val DEFAULT_ACCOUNT_NAME = "Default Account"
        const val DEFAULT_CURRENCY_CODE = "USD"
    }
}