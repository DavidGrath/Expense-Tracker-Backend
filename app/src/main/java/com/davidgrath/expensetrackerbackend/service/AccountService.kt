package com.davidgrath.expensetrackerbackend.service

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
}