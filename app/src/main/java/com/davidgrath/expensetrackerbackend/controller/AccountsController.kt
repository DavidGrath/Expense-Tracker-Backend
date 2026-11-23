package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.api.AccountsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.*
import com.davidgrath.expensetrackerbackend.service.AccountService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.accountPojoToAccount
import com.davidgrath.expensetrackerbackend.utils.accountWithStatsToAccountDetails
import com.davidgrath.expensetrackerbackend.utils.doTheOtelThing
import com.davidgrath.expensetrackerbackend.utils.validateGenericSingleLineString
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

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val accounts = accountService.getAccountsForProfile(profile.id!!)
        val apiAccounts = accounts.map { a ->
            accountPojoToAccount(a)
        }
        return ResponseEntity.ok(apiAccounts)
    }

    override fun accountsDetailsGet(profileId: @NotNull String): ResponseEntity<List<AccountDetails?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val accounts = accountService.getAccountSummariesForProfile(profile.id!!)
        val apiAccounts = accounts.map { a ->
            accountWithStatsToAccountDetails(a)
        }
        return ResponseEntity.ok(apiAccounts)
    }

    override fun accountsIdGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<AccountDetails?>? {

        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val account = accountService.getAccountSummary(profile.id!!, id)
        if(account == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Account not found", exception)
            throw exception
        }
        val apiAccounts = accountWithStatsToAccountDetails(account)
        return ResponseEntity.ok(apiAccounts)
    }

    override fun accountsIdPut(
        id: @NotNull Long,
        profileId: @NotNull String,
        accountUpdateRequest: @Valid AccountUpdateRequest?
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        validateAccountOwnership(user.id!!, profile.id!!, id)

        val nameTrimmed = accountUpdateRequest!!.name.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }

        val currency = try {
            Currency.getInstance(accountUpdateRequest!!.currencyCode)
        } catch (e: IllegalArgumentException) {
            LOGGER.warn("Wrong currency: {}", accountUpdateRequest!!.currencyCode)
            null
        }
        if(currency == null) {
            val exception = CustomHttpException(ErrorCode.INVALID_CURRENCY_CODE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Wrong currency code", exception)
            throw exception
        }
        val count = accountService.updateAccount(profile.id!!, id, nameTrimmed, currency)

        if(count == 0) {
            LOGGER.info("No changes made")
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
        }
        return ResponseEntity.ok().build()
    }

    override fun accountsPost(
        profileId: @NotNull String,
        accountCreateRequest: @Valid AccountCreateRequest?
    ): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val currency = try {
            Currency.getInstance(accountCreateRequest!!.currencyCode)
        } catch (e: IllegalArgumentException) {
            LOGGER.warn("Wrong currency: {}", accountCreateRequest!!.currencyCode)
            null
        }
        if(currency == null) {
            val exception = CustomHttpException(ErrorCode.INVALID_CURRENCY_CODE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Wrong currency code", exception)
            throw exception
        }
        val nameTrimmed = accountCreateRequest!!.name.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val id = accountService.createAccount(profile.id!!, nameTrimmed, accountCreateRequest.currencyCode!!)
        return ResponseEntity.ok(id)
    }

    override fun accountsIdCurrencyCodePatch(
        id: @NotNull Long,
        profileId: @NotNull String,
        accountCurrencyUpdateRequest: @Valid AccountCurrencyUpdateRequest?
    ): ResponseEntity<Void?>? {

        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

//        validateAccountOwnership(user.id!!, profile.id!!, id) //TODO IDor tests
        val currency = try {
            Currency.getInstance(accountCurrencyUpdateRequest!!.currencyCode)
        } catch (e: IllegalArgumentException) {
            LOGGER.warn("Wrong currency: {}", accountCurrencyUpdateRequest!!.currencyCode)
            null
        }
        if(currency == null) {
            val exception = CustomHttpException(ErrorCode.INVALID_CURRENCY_CODE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Wrong currency code", exception)
            throw exception
        }

        val count = accountService.updateAccountCurrencyCode(profile.id!!, id, currency)
        return ResponseEntity.ok(null)
    }

    override fun accountsIdDefaultPost(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<AccountDefaultUpdateResponse?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        validateAccountOwnership(user.id!!, profile.id!!, id)

        val defaultAccount = accountService.getDefaultAccountId(profile.id!!)
        LOGGER.debug("defaultAccount: {}, account: {}", defaultAccount, id)
        if(defaultAccount == id) {
            LOGGER.info("Account is already the default")
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
        }
        val (oldDefault, newDefault) = accountService.changeDefaultAccount(profile.id!!, id)
        if(oldDefault == newDefault) {
            LOGGER.info("Account is already the default")
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
        }
        val response = AccountDefaultUpdateResponse(oldDefault, newDefault)
        return ResponseEntity.ok(response)
    }


    override fun accountsIdNamePatch(
        id: @NotNull Long,
        profileId: @NotNull String,
        accountNameUpdateRequest: @Valid AccountNameUpdateRequest?
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

//        validateAccountOwnership(user.id!!, profile.id!!, id) //TODO IDOR Tests
        val nameTrimmed = accountNameUpdateRequest!!.name.trim()
        if(!validateGenericSingleLineString(nameTrimmed)) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val count = accountService.updateAccountName(profile.id!!, id, nameTrimmed)

        if(count == 0) {
            LOGGER.info("No changes made")
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
        }
        return ResponseEntity.ok().build()

    }

    private fun validateAccountOwnership(userId: Long, profileId: Long, accountId: Long) {

        if(!accountService.isAccountForUser(userId, accountId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Account not for user", exception)
            throw exception
        }
        if(!accountService.isAccountInProfile(profileId, accountId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Account not for profile", exception)
            throw exception
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountsController::class.java)
    }
}