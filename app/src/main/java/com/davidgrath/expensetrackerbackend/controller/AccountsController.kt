package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.AccountsApi
import com.davidgrath.expensetracker.generatedapi.model.Account
import com.davidgrath.expensetracker.generatedapi.model.AccountsIdPutRequest
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.service.AccountService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.accountWithStatsToAccount
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountsController: AccountsApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var accountService: AccountService

    override fun accountsGet(profileId: @NotNull String): ResponseEntity<List<Account?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val accounts = accountService.getAccountsForProfile(profile.id!!)
        val apiAccounts = accounts.map { a ->
            accountWithStatsToAccount(a)
        }
        return ResponseEntity.ok(apiAccounts)
    }

    override fun accountsIdGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<Account?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val account = accountService.getAccountSummary(profile.id!!, id)
        if(account == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Account not found", exception)
            throw exception
        }
        val apiAccounts = accountWithStatsToAccount(account)
        return ResponseEntity.ok(apiAccounts)
    }

    override fun accountsIdPut(
        id: @NotNull Long,
        profileId: @NotNull String,
        accountsIdPutRequest: @Valid AccountsIdPutRequest?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountsController::class.java)
    }
}