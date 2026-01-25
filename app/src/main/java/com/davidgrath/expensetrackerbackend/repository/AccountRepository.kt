package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.AccountWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
open class AccountRepository {

    @Autowired
    open lateinit var dslContext: DSLContext

    //region Create

    fun insertAccount(account: AccountPojo): Long {
        val accountDb = dslContext.newRecord(ACCOUNT, account)
        accountDb.store()
        return accountDb.id!!
    }

    //endregion

    //region Read
    /*fun findById(id: Long): AccountPojo? {
        val selectStep = dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }*/

    fun findByProfileIdAndId(profileId: Long, id: Long): AccountPojo? {
        val selectStep = dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }



    fun getAllByProfileId(profileId: Long): List<AccountPojo> {
        val selectStep = dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(AccountPojo::class.java)
        //TODO, also run pre-count queries for every single fetch and print whether the count exceeds the limit
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getByProfileIdAndId(profileId: Long, id: Long): AccountPojo? {
        val selectStep = dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }

    fun getByUserIdAndId(userId: Long, id: Long): AccountPojo? {
        val selectStep = dslContext.select(ACCOUNT.asterisk())
            .from(ACCOUNT)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .and(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }

    fun getAllByUserId(userId: Long): List<AccountPojo> {
        val selectStep = dslContext.select(ACCOUNT.asterisk())
            .from(ACCOUNT)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(AccountPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getAccountSummary(profileId: Long, id: Long): AccountWithStats? {
        val selectStep = dslContext
            .select(ACCOUNT.asterisk(),
                dslContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID).and(TRANSACTION.DEBIT_OR_CREDIT.notEqual(0))).asField<BigDecimal>().`as`("expenses"),
                dslContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID).and(TRANSACTION.DEBIT_OR_CREDIT.equal(0))).asField<BigDecimal>().`as`("income"),
                dslContext.select(DSL.count(TRANSACTION.ID)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID)).asField<Long>().`as`("transactionCount"),
                dslContext.select(DSL.count(TRANSACTION_ITEM.ID)).from(TRANSACTION_ITEM)
                    .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
                    .where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID))
                    .asField<Long>().`as`("itemCount")
                )
            .from(ACCOUNT)
            .where(ACCOUNT.ID.eq(id))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(AccountWithStats::class.java)
    }
    fun getAccountSummariesForProfile(profileId: Long): List<AccountWithStats> {
        val selectStep = dslContext
            .select(ACCOUNT.asterisk(),
                dslContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID).and(TRANSACTION.DEBIT_OR_CREDIT.notEqual(0))).asField<BigDecimal>().`as`("expenses"),
                dslContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID).and(TRANSACTION.DEBIT_OR_CREDIT.equal(0))).asField<BigDecimal>().`as`("income"),
                dslContext.select(DSL.count(TRANSACTION.ID)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID)).asField<Long>().`as`("transactionCount"),
                dslContext.select(DSL.count(TRANSACTION_ITEM.ID)).from(TRANSACTION_ITEM)
                    .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
                    .where(TRANSACTION.ACCOUNT_ID.eq(ACCOUNT.ID))
                    .asField<Long>().`as`("itemCount")
                )
            .from(ACCOUNT).where(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch()?.into(AccountWithStats::class.java)?:emptyList()
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    // endregion

    // region Update

    fun updateAccount(profileId: Long, id: Long, name: String): Int {
        val updateStep = dslContext.update(ACCOUNT)
            .set(ACCOUNT.NAME, name)
            .where(ACCOUNT.ID.eq(id))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }

    //endregion

    // region DELETE!!!

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(ACCOUNT).where("true")
        return delete.execute()
    }
    // endregion

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountRepository::class.java)
    }
}