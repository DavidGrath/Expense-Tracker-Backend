package com.davidgrath.expensetrackerbackend.controller.exception

import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice(annotations = [RestController::class])
class ExceptionResponseHandler {
    @ExceptionHandler(CustomHttpException::class)
    fun handleCustomHttpException(e: CustomHttpException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(e.httpStatus)
        problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, e.errorCode.code)
        return problemDetail
    }
}