package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.entities.views.AccountWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AccountService {
    @Autowired
    lateinit var accountRepository: AccountRepository

    fun isAccountInProfile(profileId: Long, accountId: Long): Boolean {
        val account = accountRepository.getByProfileIdAndId(profileId, accountId)
        return account != null
    }

    fun isAccountForUser(userId: Long, accountId: Long): Boolean {
        val account = accountRepository.getByUserIdAndId(userId, accountId)
        return account != null
    }

    fun getAccount(profileId: Long, accountId: Long): AccountPojo? {
        return accountRepository.getByProfileIdAndId(profileId, accountId)
    }

    fun getAccountsForProfile(profileId: Long): List<AccountWithStats> {
        return accountRepository.getAccountSummariesForProfile(profileId)
    }

    fun getAccountSummary(profileId: Long, accountId: Long): AccountWithStats? {
        return accountRepository.getAccountSummary(profileId, accountId)
    }
}