package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class AccountRepository {

    @Autowired
    lateinit var dslContext: DSLContext
    val ACCOUNT: Account = Account.ACCOUNT

    fun insertAccount(account: AccountPojo): Long {
        val accountDb = dslContext.newRecord(ACCOUNT, account)
        accountDb.store()
        return accountDb.id!!
    }

    fun findById(id: Long): AccountPojo? {
        val selectStep = dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }

    fun getAllByProfileId(profileId: Long): List<AccountPojo> {
        val selectStep = dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
        return selectStep.fetch().into(AccountPojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(ACCOUNT).where("true")
        return delete.execute()
    }
}