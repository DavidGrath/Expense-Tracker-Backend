package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SellerRepository @Autowired constructor(open var mainContext: DSLContext) {

    // region Create

    fun insertSeller(sellerPojo: SellerPojo): Long {
        return insertSellerTrx(mainContext, sellerPojo)
    }

    fun insertSellerTrx(trxContext: DSLContext, sellerPojo: SellerPojo): Long {
        val seller = trxContext.newRecord(SELLER, sellerPojo)
        seller.store()
        return seller.id!!
    }

    // endregion

    // region Read

    fun getByProfileIdAndId(profileId: Long, id: Long): SellerPojo? {
        val selectStep = mainContext.selectFrom(SELLER)
            .where(SELLER.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(SellerPojo::class.java)
    }

    fun getByUserIdAndId(userId: Long, id: Long): SellerPojo? {
        val selectStep = mainContext.select(SELLER.asterisk())
            .from(SELLER)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))
        return selectStep.fetchOne()?.into(SellerPojo::class.java)
    }

    fun getAllByProfileId(profileId: Long): List<SellerPojo> {
        val selectStep = mainContext.select(SELLER.asterisk())
            .from(SELLER)
            .where(SELLER.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(SellerPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    //TODO Replace get with count queries for when I want to check limits, and also getAll with get all ID queries for when I want to check for existence by user
    fun countAll(profileId: Long): Int {
        return countAllTrx(mainContext, profileId)
    }

    fun countAllTrx(trxContext: DSLContext, profileId: Long): Int {
        val selectStep = trxContext.select(DSL.count(SELLER.ID))
            .from(SELLER)
            .where(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()!!.value1()
    }

    fun updateSeller(profileId: Long, id: Long, name: String): Int {
        val updateStep = mainContext.update(SELLER)
            .set(SELLER.NAME, name)
            .where(SELLER.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))
        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerRepository::class.java)
    }
}