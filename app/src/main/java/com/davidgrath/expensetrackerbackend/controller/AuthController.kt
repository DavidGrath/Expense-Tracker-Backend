package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.AuthApi
import com.davidgrath.expensetracker.generatedapi.model.SignUpRequest
import com.davidgrath.expensetrackerbackend.configuration.CustomUserDetails
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.service.AuthService
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
open class AuthController: AuthApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var authService: AuthService

    override fun authLoginPost(): ResponseEntity<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as CustomUserDetails
        LOGGER.info("Username: {}", principal.username)
        val token = authService.createUserToken(principal.username!!)
        return ResponseEntity.ok(token)
    }

    override fun authSignUpPost(signUpRequest: SignUpRequest?): ResponseEntity<String> {
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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthController::class.java)
    }
}