package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.AccountWithStats
import com.davidgrath.expensetrackerbackend.entities.views.OrdinalSortKeyOccupancySlot
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
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
    open lateinit var mainContext: DSLContext

    //region Create

    fun insertAccount(account: AccountPojo): Long {
        return insertAccountTrx(mainContext, account)
    }
    fun insertAccountTrx(trxContext: DSLContext, account: AccountPojo): Long {
        val accountDb = trxContext.newRecord(ACCOUNT, account)
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
        val selectStep = mainContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }



    fun getAllByProfileId(profileId: Long): List<AccountPojo> {
        return getAllByProfileIdTrx(mainContext, profileId)
    }

    fun getAllByProfileIdTrx(trxContext: DSLContext, profileId: Long): List<AccountPojo> {
        val selectStep = trxContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(AccountPojo::class.java)
        //TODO, also run pre-count queries for every practical fetch and print whether the count exceeds the limit
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getByProfileIdAndId(profileId: Long, id: Long): AccountPojo? {
        return getByProfileIdAndIdTrx(mainContext, profileId, id)
    }

    fun getByProfileIdAndOrdinal(profileId: Long, ordinal: Int): AccountPojo? {
        val selectStep = mainContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(ACCOUNT.ORDINAL.eq(ordinal))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }

    fun getByProfileIdAndIdTrx(trxContext: DSLContext, profileId: Long, id: Long): AccountPojo? {
        val selectStep = trxContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .and(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }

    fun getByUserIdAndId(userId: Long, id: Long): AccountPojo? {
        val selectStep = mainContext.select(ACCOUNT.asterisk())
            .from(ACCOUNT)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .and(ACCOUNT.ID.eq(id))
        return selectStep.fetchOne()?.into(AccountPojo::class.java)
    }

    fun getAllByUserId(userId: Long): List<AccountPojo> {
        val selectStep = mainContext.select(ACCOUNT.asterisk())
            .from(ACCOUNT)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(AccountPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getAccountSummary(profileId: Long, id: Long): AccountWithStats? {
        val selectStep = mainContext
            .select(ACCOUNT.asterisk(),
                mainContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID)).and(TRANSACTION.DEBIT_OR_CREDIT.notEqual(0))).asField<BigDecimal>().`as`("expenses"),
                mainContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID)).and(TRANSACTION.DEBIT_OR_CREDIT.equal(0))).asField<BigDecimal>().`as`("income"),
                mainContext.select(DSL.count(TRANSACTION.ID)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID))).asField<Long>().`as`("transactionCount"),
                mainContext.select(DSL.count(TRANSACTION_ITEM.ID)).from(TRANSACTION_ITEM)
                    .innerJoin(TRANSACTION).on(TRANSACTION.ORDINAL.eq(TRANSACTION_ITEM.TRANSACTION_ORDINAL).and(TRANSACTION.ACCOUNT_ORDINAL.eq(TRANSACTION_ITEM.ACCOUNT_ORDINAL)).and(TRANSACTION.PROFILE_ID.eq(TRANSACTION_ITEM.PROFILE_ID)))
                    .where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID)))
                    .asField<Long>().`as`("itemCount")
                )
            .from(ACCOUNT)
            .where(ACCOUNT.ID.eq(id))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(AccountWithStats::class.java)
    }
    fun getAccountSummariesForProfile(profileId: Long): List<AccountWithStats> {
        val selectStep = mainContext
            .select(ACCOUNT.asterisk(),
                mainContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID)).and(TRANSACTION.DEBIT_OR_CREDIT.notEqual(0))).asField<BigDecimal>().`as`("expenses"),
                mainContext.select(DSL.ifnull(DSL.sum(TRANSACTION.AMOUNT), BigDecimal.ZERO)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID)).and(TRANSACTION.DEBIT_OR_CREDIT.equal(0))).asField<BigDecimal>().`as`("income"),
                mainContext.select(DSL.count(TRANSACTION.ID)).from(TRANSACTION).where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID))).asField<Long>().`as`("transactionCount"),
                mainContext.select(DSL.count(TRANSACTION_ITEM.ID)).from(TRANSACTION_ITEM)
                    .innerJoin(TRANSACTION).on(TRANSACTION.ORDINAL.eq(TRANSACTION_ITEM.TRANSACTION_ORDINAL).and(TRANSACTION.ACCOUNT_ORDINAL.eq(TRANSACTION_ITEM.ACCOUNT_ORDINAL)).and(TRANSACTION.PROFILE_ID.eq(TRANSACTION_ITEM.PROFILE_ID)))
                    .where(TRANSACTION.ACCOUNT_ORDINAL.eq(ACCOUNT.ORDINAL).and(TRANSACTION.PROFILE_ID.eq(ACCOUNT.PROFILE_ID)))
                    .asField<Long>().`as`("itemCount")
                )
            .from(ACCOUNT).where(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch()?.into(AccountWithStats::class.java)?:emptyList()
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun countDistinctCurrencyCodes(profileId: Long, accountIds: List<Long>): Int {
        val selectStep = mainContext.select(DSL.countDistinct(ACCOUNT.CURRENCY_CODE))
            .from(ACCOUNT)
            .where(ACCOUNT.ID.`in`(accountIds))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()!!.value1()
    }

    fun getMaxOrdinalSortKeyAndOccupancySlot(profileId: Long): OrdinalSortKeyOccupancySlot {
        return getMaxOrdinalSortKeyAndOccupancySlotTrx(mainContext, profileId)
    }

    fun getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext: DSLContext, profileId: Long): OrdinalSortKeyOccupancySlot {
        val selectStep = trxContext.select(
            DSL.max(ACCOUNT.ORDINAL).`as`("ordinal"),
            DSL.max(ACCOUNT.SORT_KEY).`as`("sortKey"),
            DSL.max(ACCOUNT.OCCUPANCY_SLOT).`as`("occupancySlot"),
        ).from(ACCOUNT)
            .where(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(OrdinalSortKeyOccupancySlot::class.java)!!
    }

    // endregion

    // region Update

    fun updateAccount(profileId: Long, id: Long, name: String): Int {
        val updateStep = mainContext.update(ACCOUNT)
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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountRepository::class.java)
    }
}