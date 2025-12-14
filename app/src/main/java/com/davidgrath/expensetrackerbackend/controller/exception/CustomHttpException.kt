package com.davidgrath.expensetrackerbackend.controller.exception

import org.springframework.http.HttpStatus

class CustomHttpException(val errorCode: ErrorCode, val httpStatus: HttpStatus): RuntimeException(errorCode.code) {

}