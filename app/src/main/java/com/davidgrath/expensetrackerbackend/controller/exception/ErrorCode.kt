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
    INVALID_INPUT("invalid_input")
}