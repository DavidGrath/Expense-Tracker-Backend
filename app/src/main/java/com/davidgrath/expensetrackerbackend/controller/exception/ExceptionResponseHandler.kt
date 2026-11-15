package com.davidgrath.expensetrackerbackend.controller.exception

import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.service.exceptions.EntityNotFoundException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidCurrencyCodeException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidDraftException
import com.davidgrath.expensetrackerbackend.service.exceptions.NoExplicitDefaultAccountException
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import org.springframework.http.HttpStatus
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

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND)
        problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, ErrorCode.ENTITY_NOT_FOUND.code)
        return problemDetail
    }

    @ExceptionHandler(CountLimitExceededException::class)
    fun handleCountLimitExceededException(e: CountLimitExceededException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, ErrorCode.LIMITS_EXCEEDED.code)
        return problemDetail
    }

    @ExceptionHandler(NoExplicitDefaultAccountException::class)
    fun handleNoExplicitDefaultAccountException(e: NoExplicitDefaultAccountException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, ErrorCode.SERVER_ERROR.code)
        return problemDetail
    }

    @ExceptionHandler(InvalidDraftException::class)
    fun handleInvalidDraftException(e: InvalidDraftException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, ErrorCode.INVALID_INPUT.code)
        return problemDetail
    }

    @ExceptionHandler(InvalidCurrencyCodeException::class)
    fun handleInvalidCurrencyCodeException(e: InvalidCurrencyCodeException): ProblemDetail {
        val problemDetail: ProblemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, ErrorCode.INVALID_CURRENCY_CODE.code)
        return problemDetail
    }
}