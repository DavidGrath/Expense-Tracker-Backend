package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.CustomUserDetails
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.api.AuthApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.AuthResponse
import com.davidgrath.expensetrackerbackend.generatedapi.model.SignUpRequest
import com.davidgrath.expensetrackerbackend.service.AuthService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.Constants
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
open class AuthController: AuthApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var authService: AuthService

    override fun authLoginPost(): ResponseEntity<AuthResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as CustomUserDetails
        LOGGER.info("Username: {}", principal.username)
        val token = authService.createUserToken(principal.username!!)
        return ResponseEntity.ok(token)
    }

    override fun authSignUpPost(signUpRequest: SignUpRequest?): ResponseEntity<AuthResponse> {
        val userExists = userService.doesUserExist(signUpRequest!!.username!!)
        if(userExists) {
            val exception = CustomHttpException(ErrorCode.USERNAME_TAKEN, HttpStatus.BAD_REQUEST)
            LOGGER.error("Sign up error", exception)
            throw exception
        }
        val userId = userService.createUser(signUpRequest.username!!, signUpRequest.password!!, Locale.forLanguageTag(signUpRequest.languageTag))
        LOGGER.info("Created user successfully")
        val token = authService.createUserToken(signUpRequest.username!!)
        return ResponseEntity.ok(token)
    }

    override fun authRefreshGet(refreshToken: @NotNull @Valid String): ResponseEntity<AuthResponse?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val jwt = authService.parseToken(refreshToken)
        val payload = jwt.payload
        val refreshSubject = payload.subject
        LOGGER.debug("Principal claims: {}", principal.claims)
        LOGGER.debug("Refresh claims: {}", jwt.payload)
        if(principal.subject != refreshSubject) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.UNAUTHORIZED)
            LOGGER.error("Subjects not the same", exception)
            throw exception
        }
        val type = payload.get(Constants.CUSTOM_CLAIM_TYPE, String::class.java)
        if(type != Constants.TokenType.Refresh.value) {
            val exception = CustomHttpException(ErrorCode.INVALID_TOKEN_TYPE, HttpStatus.UNAUTHORIZED)
            LOGGER.error("Refresh error", exception)
            throw exception
        }
        //TODO There's the general possibility that a token can be made for a non-existent user. Make a global handler for this contrived occurence
        val token = authService.createUserToken(principal.subject)
        return ResponseEntity.ok(token)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthController::class.java)
    }
}