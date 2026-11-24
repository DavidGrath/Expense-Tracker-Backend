package com.davidgrath.expensetrackerbackend.controller.exception

enum class ErrorCode(val code: String) {
    USERNAME_OR_PASSWORD_INCORRECT("username_or_password_incorrect"),
    USERNAME_TAKEN("username_taken"),
    ENTITY_NOT_FOUND("entity_not_found"),
    ENTITY_NOT_IN_PROFILE("entity_not_in_profile"),
    TIME_ZONE_NOT_VALID("time_zone_not_valid"),
    LIMITS_EXCEEDED("limits_exceeded"),
    CATEGORY_ICON_FORMAT_INCORRECT("category_icon_format_incorrect"),
    CATEGORY_ICON_NAME_INCORRECT("category_icon_name_incorrect"),
    CATEGORY_NOT_MODIFIABLE("category_not_modifiable"),
    INVALID_INPUT("invalid_input"), //TODO I'm contemplating splitting this up: it feels too broad at times
    DRAFT_ALREADY_EXISTS("draft_already_exists"),
    DRAFT_DOES_NOT_EXIST("draft_does_not_exist"),
    FILE_TOO_LARGE("file_too_large"),
    DRAFT_NOT_VALID("draft_not_valid"),
    DRAFT_MUST_HAVE_AT_LEAST_ONE_ITEM("draft_must_have_at_least_one_item"),
    INVALID_AUTH_TOKEN_TYPE("invalid_auth_token_type"),
    INVALID_SEARCH_QUERY("invalid_search_query"),
    INVALID_STATISTICS_FILTER("invalid_statistics_filter"),
    INVALID_CURRENCY_CODE("invalid_currency_code"),
    INVALID_WEIGHT_UNIT("invalid_weight_unit"),
    INVALID_VOLUME_UNIT("invalid_volume_unit"),
    INVALID_UNIT_PREFIX("invalid_unit_prefix"),
    SERVER_ERROR("server_error"),
    INVALID_DATE_RANGE("invalid_date_range") //TODO This is specifically for endpoints that accept from and to as query parameters instead of as a filter object. Add and test appropriately
}