package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.views.AccountWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class AccountService {

    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var accountRepository: AccountRepository

    fun isAccountInProfile(profileId: Long, accountId: Long): Boolean {
        val account = accountRepository.getByProfileIdAndId(profileId, accountId)
        val value = account != null
        LOGGER.info("Account in profile: {}", value)
        return value
    }

    fun getAccount(profileId: Long, accountId: Long): AccountPojo? {
        return accountRepository.getByProfileIdAndId(profileId, accountId)
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
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val accountPojo = AccountPojo(null, profileId, currencyCode, null, null, name, utcDate)
        val id = accountRepository.insertAccount(accountPojo)
        LOGGER.info("ID: {}", id)
        return id
    }

    fun updateAccount(profileId: Long, id: Long, name: String): Int {
        val count = accountRepository.updateAccount(profileId, id, name)
        LOGGER.info("Update result: {}", count)
        return count
    }


    fun isAccountForUser(userId: Long, accountId: Long): Boolean {
        val account = accountRepository.getByUserIdAndId(userId, accountId)
        val value = account != null
        LOGGER.info("Account for user: {}", value)
        return value
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountService::class.java)
    }
}