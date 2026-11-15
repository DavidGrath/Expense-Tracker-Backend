package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.entities.views.AccountWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.GenericRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.service.exceptions.EntityNotFoundException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidCurrencyCodeException
import com.davidgrath.expensetrackerbackend.service.exceptions.NoExplicitDefaultAccountException
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Mover
import com.davidgrath.expensetrackerbackend.utils.findEarliestOccupancySlot
import com.davidgrath.expensetrackerbackend.utils.getNextSortKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency

@Service
class AccountService {

    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var genericRepository: GenericRepository

    fun isAccountInProfile(profileId: Long, accountId: Long): Boolean {
        val account = accountRepository.getByProfileIdAndId(profileId, accountId)
        val value = account != null
        LOGGER.info("Account in profile: {}", value)
        return value
    }

    fun getAccount(profileId: Long, accountId: Long): AccountPojo? {
        return accountRepository.getByProfileIdAndId(profileId, accountId)
    }
    fun getAccountByOrdinal(profileId: Long, accountOrdinal: Short): AccountPojo? {
        return accountRepository.getByProfileIdAndOrdinal(profileId, accountOrdinal)
    }

    fun getAccountsForProfile(profileId: Long): List<AccountPojo> {
        val list = accountRepository.getAllByProfileId(profileId)
        LOGGER.info("List size: {}", list.size)
        return list
    }

    fun getAccountSummary(profileId: Long, accountId: Long): AccountWithStats? {
        return accountRepository.getAccountSummary(profileId, accountId)
    }


    fun getAccountSummariesForProfile(profileId: Long): List<AccountWithStats> {
        val list = accountRepository.getAccountSummariesForProfile(profileId)
        LOGGER.info("List size: {}", list.size)
        return list
    }

    fun createAccount(profileId: Long, name: String, currencyCode: String): Long {
        var retId = -1L
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val profile = profileRepository.getByIdForUpdateTrx(trxContext, profileId)
            LOGGER.info("Acquired lock for profile {}", profile.id)

            val existing = accountRepository.getAllByProfileIdTrx(trxContext, profile.id!!)
            if(existing.size >= Constants.Constraints.MAX_ACCOUNTS_PER_PROFILE) {
                val exception = CountLimitExceededException(Constants.Constraints.MAX_ACCOUNTS_PER_PROFILE)
                LOGGER.error("Max accounts reached", exception)
                throw exception
            }
            val exc: IllegalArgumentException? = try {
                Currency.getInstance(currencyCode)
                null
            } catch (e: IllegalArgumentException) {
                e
            }
            if(exc != null) {
                val exception = InvalidCurrencyCodeException()
                LOGGER.error("Invalid currency code", exception)
                throw exception
            }
            val ordinalSortKeyOccupancySlot = accountRepository.getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext, profileId)
            val nextOrdinal = ((ordinalSortKeyOccupancySlot.ordinal?: 0) + 1).toShort()

            val allSlots = accountRepository.getAllOccupancySlotsTrx(trxContext, profileId)
            val nextOccupancySlot = findEarliestOccupancySlot(allSlots.toShortArray(), Constants.Constraints.MAX_ACCOUNTS_PER_PROFILE)
            if(nextOccupancySlot == null) {
                throw CountLimitExceededException(Constants.Constraints.MAX_ACCOUNTS_PER_PROFILE)
            }

            val idToSortKeyMap = existing.associate { it.id!! to it.sortKey!! }
            val mover = object: Mover<Long> {
                override fun moveSortKey(id: Long, sortKey: Short) {
                    accountRepository.moveSortKeyTrx(trxContext, profileId, id, sortKey)
                }
            }

            val nextSortKey = getNextSortKey(ordinalSortKeyOccupancySlot.sortKey?:0, idToSortKeyMap, mover)

            val date = ZonedDateTime.now(timeHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
            val accountPojo = AccountPojo(null, profileId, currencyCode, null, null, name, FALSE_BYTE, nextOrdinal, nextSortKey, nextOccupancySlot, utcDate)
            val id = accountRepository.insertAccountTrx(trxContext, accountPojo)
            LOGGER.info("ID: {}", id)
            retId = id
        }
        return retId
    }

    fun updateAccount(profileId: Long, id: Long, name: String, currency: Currency): Int {
        var ret = -1
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()

            val count = accountRepository.updateAccountTrx(trxContext, profileId, id, name, currency.toString())
            //I tried getting the affected instead of matched row count, but it didn't help. Just leave this here
            val affectedCount = genericRepository.affectedCount(trxContext)
            ret = affectedCount
        }
        return ret
    }

    fun updateAccountName(profileId: Long, id: Long, name: String): Int {
        val count = accountRepository.updateAccountName(profileId, id, name)
        return count
    }


    fun updateAccountCurrencyCode(profileId: Long, id: Long, currency: Currency): Int {
        val count = accountRepository.updateAccountCurrencyCode(profileId, id, currency.toString())
        return count
    }


    fun isAccountForUser(userId: Long, accountId: Long): Boolean {
        val account = accountRepository.getByUserIdAndId(userId, accountId)
        val value = account != null
        LOGGER.info("Account for user: {}", value)
        return value
    }

    fun areAllAccountsForUser(userId: Long, accountIds: List<Long>): Boolean {
        val userAccounts = accountRepository.getAllByUserId(userId).map { it.id }
        for(acc in accountIds) {
            if(acc !in userAccounts) {
                LOGGER.info("Account {} not for user {}", acc, userId)
                return false
            }
        }
        LOGGER.info("All accounts belong to user {}", userId)
        return true
    }
    fun areAllAccountsForProfile(profileId: Long, accountIds: List<Long>): Boolean {
        val userAccounts = accountRepository.getAllByProfileId(profileId).map { it.id }
        for(acc in accountIds) {
            if(acc !in userAccounts) {
                LOGGER.info("Account {} not for profile {}", acc, profileId)
                return false
            }
        }
        LOGGER.info("All accounts belong to profile {}", profileId)
        return true
    }

    fun getDefaultAccountId(profileId: Long): Long {
        val defaultAccount = accountRepository.findDefaultAccountId(profileId)
        if(defaultAccount == null) {
            LOGGER.error("No account found")
            throw NoExplicitDefaultAccountException()
        }
        return defaultAccount
    }


    fun changeDefaultAccount(profileId: Long, accountId: Long): Pair<Long?, Long> {
        var pair: Pair<Long?, Long> = null to -1L
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val currentDefaultAccount = accountRepository.getDefaultAccountForUpdateTrx(trxContext, profileId)
            if(currentDefaultAccount == null) {
                LOGGER.error("Account not found")
                throw EntityNotFoundException()
            }
            LOGGER.info("Acquired lock for default account {}", currentDefaultAccount.id)
            val newAccount = accountRepository.findByProfileIdAndIdForUpdateTrx(trxContext, profileId, accountId)
            if(newAccount == null) {
                LOGGER.error("New account not found")
                throw EntityNotFoundException()
            }
            LOGGER.info("Acquired lock for new account {}", newAccount.id)
            if(newAccount.id == currentDefaultAccount.id) {
                LOGGER.info("New account is the same as the old account")
                pair = newAccount.id!! to newAccount.id!!
                return@transactionContext
            }
            accountRepository.setAccountIsDefaultTrx(trxContext, profileId, currentDefaultAccount.id!!, false)
            accountRepository.setAccountIsDefaultTrx(trxContext, profileId, newAccount.id!!, true)
            pair = currentDefaultAccount.id to newAccount.id!!
        }
        return pair
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountService::class.java)
    }
}