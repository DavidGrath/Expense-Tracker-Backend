package com.davidgrath.expensetrackerbackend.configuration.filter

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponse
import org.springframework.web.ErrorResponseException
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite


class TokenTypeFilter(val handlerExceptionResolver: HandlerExceptionResolver): OncePerRequestFilter() {


    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
//        if(principal !is Jwt) {
//            filterChain.doFilter(request, response)
//            return
//        }
        val type = principal.getClaimAsString(Constants.CUSTOM_CLAIM_TYPE)
        LOGGER.debug("JWT claims: {}", principal.claims)
        if(type != Constants.TokenType.Access.value) {
//            val exception = CustomHttpException(ErrorCode.INVALID_TOKEN_TYPE, HttpStatus.UNAUTHORIZED)
            LOGGER.error("Access error: {}", ErrorCode.INVALID_TOKEN_TYPE)
            val problemDetail: ProblemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED)
            problemDetail.setProperty(CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE, ErrorCode.INVALID_TOKEN_TYPE.code)
            val errorResponse = ErrorResponseException( HttpStatus.UNAUTHORIZED, problemDetail, null)
            response.status = HttpStatus.UNAUTHORIZED.value()
            handlerExceptionResolver.resolveException(request, response, null, errorResponse)

        } else {
            filterChain.doFilter(request, response)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TokenTypeFilter::class.java)
    }
}