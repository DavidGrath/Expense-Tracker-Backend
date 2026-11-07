package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER_LOCATION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SellerLocationRepository @Autowired constructor(open var dslContext: DSLContext) {

    // region Create

    fun insertSellerLocation(sellerLocationPojo: SellerLocationPojo): Long {
        val sellerLocation = dslContext.newRecord(SELLER_LOCATION, sellerLocationPojo)
        sellerLocation.store()
        return sellerLocation.id!!
    }

    // endregion

    // region Read

    fun getByProfileIdAndId(profileId: Long, id: Long): SellerLocationPojo? {
        val selectStep = dslContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .where(SELLER_LOCATION.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(SellerLocationPojo::class.java)
    }

    fun getByUserIdAndId(userId: Long, id: Long): SellerLocationPojo? {
        val selectStep = dslContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER_LOCATION.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))
        return selectStep.fetchOne()?.into(SellerLocationPojo::class.java)
    }

    fun getByProfileIdAndSellerId(profileId: Long, sellerId: Long): List<SellerLocationPojo> {
        val selectStep = dslContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .where(SELLER_LOCATION.SELLER_ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(SellerLocationPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun countByProfileIdAndSellerId(profileId: Long, sellerId: Long): Int {
        val selectStep = dslContext.select(DSL.count(SELLER_LOCATION.ID))
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .where(SELLER_LOCATION.SELLER_ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()!!.value1()
    }

    fun getMaxOrdinalForSeller(profileId: Long, sellerId: Long): Int? {
        val selectStep = dslContext.select(DSL.max(SELLER_LOCATION.ORDINAL))
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER.ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.value1()
    }

    fun getAllByProfileId(profileId: Long): List<SellerLocationPojo> {
        val selectStep = dslContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .where(SELLER.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(SellerLocationPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    // endregion

    // region Update

    fun updateSellerLocation(profileId: Long, id: Long, name: String): Int {
        val updateStep = dslContext.update(SELLER_LOCATION)
            .set(SELLER_LOCATION.NAME, name)
            .from(SELLER_LOCATION.innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID)))
            .where(SELLER_LOCATION.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }

    // endregion


    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerLocationRepository::class.java)
    }
}