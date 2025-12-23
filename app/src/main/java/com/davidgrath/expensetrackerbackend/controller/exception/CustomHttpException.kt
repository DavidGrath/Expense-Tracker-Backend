package com.davidgrath.expensetrackerbackend.controller.exception

import org.springframework.http.HttpStatus

/**
 * Intended to only be thrown by `Controller`s
 */
class CustomHttpException(val errorCode: ErrorCode, val httpStatus: HttpStatus): RuntimeException(errorCode.code) {

}