package com.davidgrath.expensetrackerbackend.controller.exception

import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice(annotations = [RestController::class])
class ExceptionResponseHandler {
    @ExceptionHandler(CustomHttpException::class)
    fun handleCustomHttpException(e: CustomHttpException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(e.httpStatus)
        problemDetail.setProperty("errorCode", e.errorCode.code)
        return problemDetail
    }
}