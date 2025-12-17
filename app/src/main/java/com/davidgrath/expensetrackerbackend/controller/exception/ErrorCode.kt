package com.davidgrath.expensetrackerbackend.controller.exception

enum class ErrorCode(val code: String) {
    USERNAME_OR_PASSWORD_INCORRECT("username_or_password_incorrect"),
    USERNAME_TAKEN("username_taken"),
    ENTITY_NOT_FOUND("entity_not_found"),
    ENTITY_NOT_IN_PROFILE("entity_not_in_profile"),
    INVALID_INPUT("invalid_input")
}