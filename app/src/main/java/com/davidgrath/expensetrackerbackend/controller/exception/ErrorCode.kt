package com.davidgrath.expensetrackerbackend.controller.exception

enum class ErrorCode(val code: String) {
    USERNAME_OR_PASSWORD_INCORRECT("username_or_password_incorrect"),
    USERNAME_TAKEN("username_taken")
}